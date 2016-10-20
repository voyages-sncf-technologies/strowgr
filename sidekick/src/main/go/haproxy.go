/*
 *  Copyright (C) 2016 VSCT
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package sidekick

import (
	"bytes"
	"errors"
	"fmt"
	log "github.com/Sirupsen/logrus"
	"os"
	"os/exec"
	"strconv"
	"strings"
	"syscall"
	"time"
)

type Command func(name string, arg ...string) ([]byte, error)
type Signal func(pid int, signal os.Signal) error
type Dumper func(context Context, filename string, newConf []byte)

func NewHaproxy(properties *Config, context Context) *Haproxy {
	return &Haproxy{
		properties: properties,
		Context:    context,
		Command:    execCommand,
		Signal:     osSignal,
		Dumper:     dumpConfiguration,
		Directories: NewDirectories(context, map[string]string{
			"Platform":      properties.HapHome + "/" + context.Application + "/" + context.Platform,
			"Config":        properties.HapHome + "/" + context.Application + "/Config",
			"Logs":          properties.HapHome + "/" + context.Application + "/" + context.Platform + "/logs",
			"Scripts":       properties.HapHome + "/" + context.Application + "/scripts",
			"VersionMinus1": properties.HapHome + "/" + context.Application + "/version-1",
			"Errors":        properties.HapHome + "/" + context.Application + "/" + context.Platform + "/errors",
			"Dump":          properties.HapHome + "/" + context.Application + "/" + context.Platform + "/dump",
			"Syslog":        properties.HapHome + "/SYSLOG/Config/syslog.conf.d"}),
		Files: NewPath(context,
			properties.HapHome + "/" + context.Application + "/Config/hap" + context.Application + context.Platform + ".conf",
			properties.HapHome + "/SYSLOG/Config/syslog.conf.d/syslog" + context.Application + context.Platform + ".conf",
			properties.HapHome + "/" + context.Application + "/version-1/hap" + context.Application + context.Platform + ".conf",
			properties.HapHome + "/" + context.Application + "/" + context.Platform + "/logs/haproxy.pid",
			properties.HapHome + "/" + context.Application + "/scripts/hap" + context.Application + context.Platform,
			properties.HapHome + "/" + context.Application + "/version-1/hap" + context.Application + context.Platform,
		),
	}
}

type Haproxy struct {
	properties  *Config
	State       int
	Context     Context
	Command     Command
	Directories Directories // TODO merge directories and files models
	Files       Files
	Dumper      Dumper
	Signal      Signal
}

const (
	SUCCESS int = iota
	UNCHANGED int = iota
	ERR_SYSLOG int = iota
	ERR_CONF int = iota
	ERR_RELOAD int = iota
	MAX_STATUS int = iota
)

// ApplyConfiguration write the new configuration and reload
// A rollback is called on failure
func (hap *Haproxy) ApplyConfiguration(data *EventMessageWithConf) (int, error) {
	present := false
	for _, version := range hap.properties.HapVersions {
		if version == data.Conf.Version {
			present = true
			break
		}
	}
	// validate that received haproxy configuration contains a managed version of haproxy
	if data.Conf.Version == "" || !present || data.Conf.Haproxy == nil {
		hap.Context.Fields(log.Fields{"given haproxy version": data.Conf.Version, "managed versions by sidekick": strings.Join(hap.properties.HapVersions, ",")}).Error("received configuration hasn't haproxy version or one which has not been configured in this sidekick instance or configuration is missing")
		return ERR_CONF, errors.New("received configuration hasn't haproxy version or one which has not been configured in this sidekick instance")
	}

	hap.Directories.mkDirs(hap.Context)

	// get new conf
	newConf := data.Conf.Haproxy
	hap.dumpDebug(newConf)

	// Check conf diff
	if hap.Files.Checker(hap.Files.Config) {
		// a configuration file already exists, should be archived
		if oldConf, err := hap.Files.readConfig(); err != nil {
			return ERR_CONF, err
		} else {
			if bytes.Equal(oldConf, newConf) {
				hap.Context.Fields(log.Fields{"id": hap.properties.Id}).Debug("Unchanged configuration")
				return UNCHANGED, nil
			}
			// Archive previous configuration
			hap.Files.archive()
		}
	}

	// Change configuration and link to new version
	hap.Files.linkNewVersion(data.Conf.Version)
	if err := hap.Files.writeConfig(newConf); err != nil {
		return ERR_CONF, err
	}

	hap.Context.Fields(log.Fields{"id": hap.properties.Id, "path": hap.Files.Config}).Info("New configuration written")

	// Reload haproxy
	if err := hap.reload(data.Header.CorrelationId); err != nil {
		hap.Context.Fields(log.Fields{"id": hap.properties.Id}).WithError(err).Error("Reload failed")
		hap.dumpError(newConf)
		if errRollback := hap.rollback(data.Header.CorrelationId); errRollback != nil {
			log.WithError(errRollback).Error("error in rollback in addition to error of the reload")
		} else {
			hap.Context.Fields(log.Fields{}).Debug("rollback done")
		}
		return ERR_RELOAD, err
	}

	// Write syslog fragment
	if err := hap.Files.writeSyslog(data.Conf.Syslog); err != nil {
		hap.Context.Fields(log.Fields{"id": hap.properties.Id}).WithError(err).Error("Failed to write syslog fragment")
		// TODO Should we rollback on syslog error ?
		return ERR_SYSLOG, err
	}
	hap.Context.Fields(log.Fields{"id": hap.properties.Id, "content": string(data.Conf.Syslog), "filename": hap.Files.Syslog}).Debug("Write syslog fragment")

	return SUCCESS, nil
}

func (hap *Haproxy) dumpDebug(newConf []byte) {
	if log.GetLevel() == log.DebugLevel {
		hap.Dumper(hap.Context, hap.Directories.Map["Debug"] + "/" + time.Now().Format("20060102150405") + ".log", newConf)
	}
}

func (hap *Haproxy) dumpError(newConf []byte) {
	hap.Dumper(hap.Context, hap.Directories.Map["Errors"] + "/" + time.Now().Format("20060102150405") + ".log", newConf)
}

// reload calls external shell script to reload haproxy
// It returns error if the reload fails
func (hap *Haproxy) reload(correlationId string) error {
	configurationExists := hap.Files.Checker(hap.Files.Pid)
	if configurationExists {
		pid, err := hap.Files.readPid()
		if err != nil {
			hap.Context.Fields(log.Fields{"pid path": hap.Files.Pid}).Error("can't read pid file")
			return err
		}
		hap.Context.Fields(log.Fields{"reloadScript": hap.Files.Bin, "confPath": hap.Files.Config, "pidPath": hap.Files.Pid, "pid": strings.TrimSpace(string(pid))}).Debug("reload haproxy")

		if output, err := hap.Command(hap.Files.Bin, "-f", hap.Files.Config, "-p", hap.Files.Pid, "-sf", strings.TrimSpace(string(pid))); err == nil {
			hap.Context.Fields(log.Fields{"id": hap.properties.Id, "reloadScript": hap.Files.Bin, "output": string(output[:])}).Debug("Reload succeeded")
		} else {
			hap.Context.Fields(log.Fields{"output": string(output[:])}).WithError(err).Error("Error reloading")
			return err
		}
	} else {
		hap.Context.Fields(log.Fields{"reloadScript": hap.Files.Bin, "confPath": hap.Files.Config, "pid file":hap.Files.Pid}).Info("load haproxy")
		output, err := hap.Command(hap.Files.Bin, "-f", hap.Files.Config, "-p", hap.Files.Pid)
		if err == nil {
			hap.Context.Fields(log.Fields{"id": hap.properties.Id, "reloadScript": hap.Files.Bin, "output": string(output[:])}).Debug("Reload succeeded")
		} else {
			hap.Context.Fields(log.Fields{"output": string(output[:])}).WithError(err).Error("Error reloading")
			return err
		}
	}
	return nil
}

// rollback reverts configuration files and call for reload
func (hap *Haproxy) rollback(correlationId string) error {
	if err := hap.Files.rollback(); err != nil {
		return err
	}
	return hap.reload(correlationId)
}

func (hap *Haproxy) Delete() error {
	// remove bin and config files
	err := hap.Files.removeAll()
	if err != nil {
		hap.Context.Fields(log.Fields{}).WithError(err).Error("can't delete a file")
		return err
	}
	// remove platform directory (logs, dump, errors)
	err = hap.Directories.removePlatform()
	if err != nil {
		hap.Context.Fields(log.Fields{}).WithError(err).Error("Failed to delete haproxy")
	}
	return err
}

func (hap *Haproxy) Stop() error {
	pid, err := hap.Files.readPid()
	if err != nil {
		hap.Context.Fields(log.Fields{"pid file": hap.Files.Pid}).Error("can't read pid file")
	}
	pidInt, err := strconv.Atoi(strings.TrimSpace(string(pid)))
	if err != nil {
		hap.Context.Fields(log.Fields{"pid":strings.TrimSpace(string(pid)), "pid file": hap.Files.Pid}).Error("can't convert pid to int")
		return err
	}
	err = hap.Signal(pidInt, syscall.SIGTERM)

	if err != nil {
		hap.Context.Fields(log.Fields{}).WithError(err).Error("SIGTERM sent to haproxy process fails")
	}
	return err
}

/////////////////////////
// OS implementations  //
/////////////////////////

func execCommand(name string, arg ...string) ([]byte, error) {
	return exec.Command(name, arg...).Output()
}

func osSignal(pid int, signal os.Signal) error {
	process, err := os.FindProcess(pid)
	if err != nil {
		return err
	}
	return process.Signal(signal)
}

// dumpConfiguration dumps the new configuration file with context for debugging purpose
func dumpConfiguration(context Context, filename string, newConf []byte) {
	f, err2 := os.Create(filename)
	defer f.Close()
	if err2 == nil {
		f.WriteString("================================================================\n")
		f.WriteString(fmt.Sprintf("application: %s\n", context.Application))
		f.WriteString(fmt.Sprintf("platform: %s\n", context.Platform))
		f.WriteString(fmt.Sprintf("correlationId: %s\n", context.CorrelationId))
		f.WriteString("================================================================\n")
		f.Write(newConf)
		f.Sync()

		context.Fields(log.Fields{"filename": filename}).Info("Dump configuration")
	}
}

func (hap Haproxy) Fake() bool {
	return false
}
