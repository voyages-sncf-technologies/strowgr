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
	"time"
	"strings"
	"os/exec"
)

type Command func(name string, arg ...string) ([]byte, error)
type Dumper func(context Context, filename string, newConf []byte)

func NewHaproxy(properties *Config, context Context) *Haproxy {
	return &Haproxy{
		properties: properties,
		Context:    context,
		Command: ExecCommand,
		Dumper: dumpConfiguration,
		Directories: NewDirectories(context, map[string]string{
			"Config": properties.HapHome + "/" + context.Application + "/Config",
			"Logs": properties.HapHome + "/" + context.Application + "/logs/" + context.Application + context.Platform,
			"Scripts": properties.HapHome + "/" + context.Application + "/scripts",
			"VersionMinus1": properties.HapHome + "/" + context.Application + "/version-1",
			"Errors": properties.HapHome + "/" + context.Application + "/errors",
			"Dump": properties.HapHome + "/" + context.Application + "/dump",
			"Syslog":     properties.HapHome + "/SYSLOG/Config/syslog.conf.d"}),
		Files:  NewPath(context,
			properties.HapHome + "/" + context.Application + "/Config/hap" + context.Application + context.Platform + ".conf",
			properties.HapHome + "/SYSLOG/Config/syslog.conf.d/syslog" + context.Application + context.Platform + ".conf",
			properties.HapHome + "/" + context.Application + "/version-1/hap" + context.Application + context.Platform + ".conf",
			properties.HapHome + "/" + context.Application + "/logs/" + context.Application + context.Platform + "/haproxy.pid",
			fmt.Sprintf("%s/%s/scripts/hap%s%s", properties.HapHome, context.Application, context.Application, context.Platform)),
	}
}

type Haproxy struct {
	properties  *Config
	State       int
	Context     Context
	Command     Command
	Directories Directories
	Files       Files
	Dumper      Dumper
}

func ExecCommand(name string, arg ...string) ([]byte, error) {
	return exec.Command(name, arg...).Output()
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
	if data.Conf.Version == "" || !present {
		hap.Context.Fields(log.Fields{"given haproxy version":data.Conf.Version, "managed versions by sidekick": strings.Join(hap.properties.HapVersions, ",")}).Error("received configuration hasn't haproxy version or one which has not been configured in this sidekick instance")
		return ERR_CONF, errors.New("received configuration hasn't haproxy version or one which has not been configured in this sidekick instance")
	}

	hap.Directories.mkDirs(hap.Context)
	hap.Files.linkNewVersion(data.Conf.Version)

	// get new conf
	newConf := data.Conf.Haproxy
	hap.dumpDebug(newConf)

	// Check conf diff
	oldConf, err := hap.Files.readConfig()
	if bytes.Equal(oldConf, newConf) {
		hap.Context.Fields(log.Fields{"id": hap.properties.Id}).Debug("Unchanged configuration")
		return UNCHANGED, nil
	}

	// Archive previous configuration
	hap.Files.archive()

	hap.Context.Fields(log.Fields{"id": hap.properties.Id, "archivePath": hap.Files.Archive }).Info("Old configuration saved")
	err = hap.Files.writeConfig(newConf)

	if err != nil {
		return ERR_CONF, err
	}

	hap.Context.Fields(log.Fields{"id": hap.properties.Id, "path": hap.Files.Config, }).Info("New configuration written")

	// Reload haproxy
	err = hap.reload(data.Header.CorrelationId)
	if err != nil {
		hap.Context.Fields(log.Fields{"id": hap.properties.Id}).WithError(err).Error("Reload failed")
		hap.dumpError(newConf)
		errRollback := hap.rollback(data.Header.CorrelationId)
		if errRollback != nil {
			log.WithError(errRollback).Error("error in rollback in addition to error of the reload")
		} else {
			hap.Context.Fields(log.Fields{}).Debug("rollback done")
		}
		return ERR_RELOAD, err
	}
	// Write syslog fragment
	err = hap.Files.writeSyslog(data.Conf.Syslog)

	if err != nil {
		hap.Context.Fields(log.Fields{"id": hap.properties.Id}).WithError(err).Error("Failed to write syslog fragment")
		// TODO Should we rollback on syslog error ?
		return ERR_SYSLOG, err
	}
	hap.Context.Fields(log.Fields{"id": hap.properties.Id, "content": string(data.Conf.Syslog), "filename": hap.Files.Syslog}).Debug("Write syslog fragment")

	return SUCCESS, nil
}

func (hap *Haproxy) dumpDebug(newConf []byte) {
	if log.GetLevel() == log.DebugLevel {
		baseDir := hap.properties.HapHome + "/" + hap.Context.Application + "/dump"
		prefix := time.Now().Format("20060102150405")
		debugPath := baseDir + "/" + prefix + "_" + hap.Context.Application + hap.Context.Platform + ".log"
		hap.Dumper(hap.Context, debugPath, newConf)
	}
}

func (hap *Haproxy) dumpError(newConf []byte) {
	baseDir := hap.properties.HapHome + "/" + hap.Context.Application + "/errors"
	prefix := time.Now().Format("20060102150405")
	errorPath := baseDir + "/" + prefix + "_" + hap.Context.Application + hap.Context.Platform + ".log"

	hap.Dumper(hap.Context, errorPath, newConf)
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

// reload calls external shell script to reload haproxy
// It returns error if the reload fails
func (hap *Haproxy) reload(correlationId string) error {
	pid, err := hap.Files.readPid()
	if err != nil {
		hap.Context.Fields(log.Fields{"pid path": string(hap.Files.Pid)}).Error("can't read pid file")
		return err
	}
	hap.Context.Fields(log.Fields{"reloadScript":hap.Files.Bin, "confPath":hap.Files.Config, "pidPath":hap.Files.Pid, "pid":string(pid)}).Debug("reload haproxy")
	output, err := hap.Command(hap.Files.Bin, "-f", hap.Files.Config, "-p", hap.Files.Pid, "-sf", string(pid))

	if err == nil {
		hap.Context.Fields(log.Fields{"id":hap.properties.Id, "reloadScript": hap.Files.Bin, "output": string(output[:]) }).Debug("Reload succeeded")
	} else {
		hap.Context.Fields(log.Fields{"output": string(output[:])}).WithError(err).Error("Error reloading")

	}
	return err
}

// rollback reverts configuration files and call for reload
func (hap *Haproxy) rollback(correlationId string) error {
	if hap.Files.archiveExists() {
		return errors.New("No configuration file to rollback")
	}
	// TODO remove current hap.confPath() ?
	hap.Files.rollback()
	hap.reload(correlationId)
	return nil
}


// getReloadScript calculates reload script path given the hap context
// It returns the full script path
func (hap *Haproxy) getReloadScript() string {
	return fmt.Sprintf("%s/%s/scripts/hap%s%s", hap.properties.HapHome, hap.Context.Application, hap.Context.Application, hap.Context.Platform)
}

func (hap *Haproxy) Delete() error {
	baseDir := hap.properties.HapHome + "/" + hap.Context.Application
	err := os.RemoveAll(baseDir)
	if err != nil {
		hap.Context.Fields(log.Fields{"dir": baseDir}).WithError(err).Error("Failed to delete haproxy")
	} else {
		hap.Context.Fields(log.Fields{"dir": baseDir}).WithError(err).Info("HAproxy deleted")
	}

	return err
}

func (hap *Haproxy) Stop() error {
	reloadScript := hap.getReloadScript()
	output, err := hap.Command("sh", reloadScript, "stop")
	if err != nil {
		hap.Context.Fields(log.Fields{}).WithError(err).Error("Error stop")
	} else {
		hap.Context.Fields(log.Fields{"reloadScript": reloadScript, "cmd":string(output[:])}).Debug("Stop succeeded")
	}
	return err
}

func (hap Haproxy) Fake() bool {
	return false
}