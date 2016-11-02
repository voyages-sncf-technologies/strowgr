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


// Haproxy manager for a given Application/Platform
type Haproxy struct {
				// config of sidekick
	Config      *Config
				// context of this haproxy (current application/platform/correlationid etc...)
	Context     Context
				// files managed by sidekick for this haproxy instance
	Files       Files
				// directories managed by sidekick for this haproxy instance
	Directories Directories // TODO merge directories and files models to a more generic 'filesystem' model
				// command wrapping real command execution
	Command     Command
				// dump abstraction to a file
	Dumper      Dumper
				// signal abstraction for sending signal
	Signal      Signal
}

func NewHaproxy(properties *Config, context Context) *Haproxy {
	// TODO manage a cache of Haproxy
	return &Haproxy{
		Config: properties,
		Context:    context,
		Command:    execCommand,
		Signal:     osSignal,
		Dumper:     dumpConfiguration,
		Directories: NewDirectories(context, map[string]string{// TODO more generic 'filesystem' model for avoiding code repetition
			"Platform":      properties.HapHome + "/" + context.Application + "/" + context.Platform,
			"Config":        properties.HapHome + "/" + context.Application + "/Config",
			"Logs":          properties.HapHome + "/" + context.Application + "/logs/" + context.Application + context.Platform,
			"Scripts":       properties.HapHome + "/" + context.Application + "/scripts",
			"VersionMinus1": properties.HapHome + "/" + context.Application + "/version-1",
			"Errors":        properties.HapHome + "/" + context.Application + "/" + context.Platform + "/errors",
			"Dump":          properties.HapHome + "/" + context.Application + "/" + context.Platform + "/dump",
			"Syslog":        properties.HapHome + "/SYSLOG/Config/syslog.conf.d"}),
		Files: NewPath(context,
			properties.HapHome + "/" + context.Application + "/Config/hap" + context.Application + context.Platform + ".conf",
			properties.HapHome + "/SYSLOG/Config/syslog.conf.d/syslog" + context.Application + context.Platform + ".conf",
			properties.HapHome + "/" + context.Application + "/version-1/hap" + context.Application + context.Platform + ".conf",
			properties.HapHome + "/" + context.Application + "/logs/" + context.Application + context.Platform + "/haproxy.pid",
			properties.HapHome + "/" + context.Application + "/scripts/hap" + context.Application + context.Platform,
			properties.HapHome + "/" + context.Application + "/version-1/hap" + context.Application + context.Platform,
		),
	}
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
func (hap *Haproxy) ApplyConfiguration(event *EventMessageWithConf) (int, error) {
	// validate version
	if event.Conf.Version == "" || !hap.isManagedVersion(event.Conf.Version) || event.Conf.Haproxy == nil {
		hap.Context.Fields(log.Fields{"given haproxy version": event.Conf.Version, "managed versions by sidekick": strings.Join(hap.Config.HapVersions, ",")}).Error("received configuration hasn't haproxy version or one which has not been configured in this sidekick instance or configuration is missing")
		return ERR_CONF, errors.New("received configuration hasn't haproxy version or one which has not been configured in this sidekick instance")
	}

	// create skeleton of directories
	hap.Directories.mkDirs(hap.Context)

	// dump received haproxy configuration in debug mode
	hap.dumpDebug(event.Conf.Haproxy)

	// Check conf diff
	if hap.Files.Checker(hap.Files.Config) {
		// a configuration file already exists, should be archived
		if oldConf, err := hap.Files.readConfig(); err != nil {
			return ERR_CONF, err
		} else if bytes.Equal(oldConf, event.Conf.Haproxy) {
			hap.Context.Fields(log.Fields{"id": hap.Config.Id}).Debug("Unchanged configuration")
			return UNCHANGED, nil
		} else {
			// Archive previous configuration
			hap.Files.archive()
		}
	}

	// Change configuration and link to new version
	hap.Files.linkNewVersion(event.Conf.Version)
	if err := hap.Files.writeConfig(event.Conf.Haproxy); err != nil {
		return ERR_CONF, err
	}

	hap.Context.Fields(log.Fields{"id": hap.Config.Id, "path": hap.Files.Config}).Info("New configuration written")

	// Reload haproxy
	if err := hap.reload(event.Header.CorrelationId); err != nil {
		hap.Context.Fields(log.Fields{"id": hap.Config.Id}).WithError(err).Error("Reload failed")
		hap.dumpError(event.Conf.Haproxy)
		if errRollback := hap.rollback(event.Header.CorrelationId); errRollback != nil {
			log.WithError(errRollback).Error("error in rollback in addition to error of the reload")
		} else {
			hap.Context.Fields(log.Fields{}).Debug("rollback done")
		}
		return ERR_RELOAD, err
	}

	// Write syslog fragment
	if err := hap.Files.writeSyslog(event.Conf.Syslog); err != nil {
		hap.Context.Fields(log.Fields{"id": hap.Config.Id}).WithError(err).Error("Failed to write syslog fragment")
		// TODO Should we rollback on syslog error ?
		return ERR_SYSLOG, err
	}
	hap.Context.Fields(log.Fields{"id": hap.Config.Id, "content": string(event.Conf.Syslog), "filename": hap.Files.Syslog}).Debug("Write syslog fragment")

	return SUCCESS, nil
}

// is a managed version by sidekick
func (hap *Haproxy) isManagedVersion(version string) bool {
	isManagedVersion := false
	for _, currentVersion := range hap.Config.HapVersions {
		if currentVersion == version {
			isManagedVersion = true
			break
		}
	}
	return isManagedVersion
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
			hap.Context.Fields(log.Fields{"id": hap.Config.Id, "reloadScript": hap.Files.Bin, "output": string(output[:])}).Debug("Reload succeeded")
		} else {
			hap.Context.Fields(log.Fields{"output": string(output[:])}).WithError(err).Error("Error reloading")
			return err
		}
	} else {
		hap.Context.Fields(log.Fields{"reloadScript": hap.Files.Bin, "confPath": hap.Files.Config, "pid file":hap.Files.Pid}).Info("load haproxy")
		output, err := hap.Command(hap.Files.Bin, "-f", hap.Files.Config, "-p", hap.Files.Pid)
		if err == nil {
			hap.Context.Fields(log.Fields{"id": hap.Config.Id, "reloadScript": hap.Files.Bin, "output": string(output[:])}).Debug("Reload succeeded")
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
//         types       //
/////////////////////////

// type abstraction for exec.Command(...).Output()
type Command func(name string, arg ...string) ([]byte, error)
// type abstraction for os.Signal
type Signal func(pid int, signal os.Signal) error
// type abstraction for dumping content to a file
type Dumper func(context Context, filename string, newConf []byte)

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
