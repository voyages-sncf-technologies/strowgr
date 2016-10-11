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
	"io/ioutil"
	"os"
	"time"
	"strings"
	"os/exec"
)

type Command func(name string, arg ...string) ([]byte, error)
type Paths struct {
	Config  string
	Syslog  string
	Archive string
	Pid     string
}

type ConfigDir map[string]string

func NewHaproxy(properties *Config, context Context) *Haproxy {
	return &Haproxy{
		properties: properties,
		Context:    context,
		Command: ExecCommand,
		ConfigDir: ConfigDir{
			"Config": properties.HapHome + "/" + context.Application + "/Config",
			"Logs": properties.HapHome + "/" + context.Application + "/logs/" + context.Application + context.Platform,
			"Scripts": properties.HapHome + "/" + context.Application + "/scripts",
			"VersionMinus1": properties.HapHome + "/" + context.Application + "/version-1",
			"Errors": properties.HapHome + "/" + context.Application + "/errors",
			"Dump": properties.HapHome + "/" + context.Application + "/dump",
			"Syslog":     properties.HapHome + "/SYSLOG/Config/syslog.conf.d",
		},
		HaproxyBinLink:  fmt.Sprintf("%s/%s/scripts/hap%s%s", properties.HapHome, context.Application, context.Application, context.Platform),
		Paths: Paths{
			Syslog: properties.HapHome + "/SYSLOG/Config/syslog.conf.d" + context.Application + context.Platform + ".conf",
			Config:           properties.HapHome + "/" + context.Application + "/Config",
			Pid: properties.HapHome + "/" + context.Application + "/logs/" + context.Application + context.Platform + "/haproxy.pid",
			Archive: properties.HapHome + "/" + context.Application + "/version-1/hap" + context.Application + context.Platform + ".conf",
		},
	}
}

type Haproxy struct {
	properties     *Config
	State          int
	Context        Context
	Command        Command
	ConfigDir      ConfigDir
	HaproxyBinLink string
	Paths          Paths
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
		log.WithFields(log.Fields{
			"given haproxy version":data.Conf.Version,
			"managed versions by sidekick": strings.Join(hap.properties.HapVersions, ",")}).Error("received configuration hasn't haproxy version or one which has not been configured in this sidekick instance")
		return ERR_CONF, errors.New("received configuration hasn't haproxy version or one which has not been configured in this sidekick instance")
	}

	hap.ConfigDir.createSkeleton(hap.Context)
	updateSymlink(hap.Context, fmt.Sprintf("/export/product/haproxy/product/%s/bin/haproxy", data.Conf.Version), hap.HaproxyBinLink)

	// get new conf
	newConf := data.Conf.Haproxy
	if log.GetLevel() == log.DebugLevel {
		hap.dumpConfiguration(hap.NewDebugPath(), newConf, data)
	}

	// Check conf diff
	oldConf, err := ioutil.ReadFile(hap.Paths.Config)
	if bytes.Equal(oldConf, newConf) {
		log.WithFields(hap.Context.Fields()).WithFields(
			log.Fields{"id": hap.properties.Id}).Debug("Unchanged configuration")
		return UNCHANGED, nil
	}

	// Archive previous configuration
	os.Rename(hap.Paths.Config, hap.Paths.Archive)
	log.WithFields(hap.Context.Fields()).WithFields(
		log.Fields{
			"id":        hap.properties.Id,
			"archivePath": hap.Paths.Archive,
		}).Info("Old configuration saved")
	err = ioutil.WriteFile(hap.Paths.Config, newConf, 0644)
	if err != nil {
		return ERR_CONF, err
	}

	log.WithFields(hap.Context.Fields()).WithFields(log.Fields{
		"id": hap.properties.Id,
		"path": hap.Paths.Config,
	}).Info("New configuration written")

	// Reload haproxy
	err = hap.reload(data.Header.CorrelationId)
	if err != nil {
		log.WithFields(hap.Context.Fields()).WithFields(log.Fields{
			"id": hap.properties.Id,
		}).WithError(err).Error("Reload failed")
		hap.dumpConfiguration(hap.NewErrorPath(), newConf, data)
		errRollback := hap.rollback(data.Header.CorrelationId)
		if errRollback != nil {
			log.WithError(errRollback).Error("error in rollback in addition to error of the reload")
		} else {
			log.WithFields(hap.Context.Fields()).Debug("rollback done")
		}
		return ERR_RELOAD, err
	}
	// Write syslog fragment
	err = ioutil.WriteFile(hap.Paths.Syslog, data.Conf.Syslog, 0644)
	if err != nil {
		log.WithFields(hap.Context.Fields()).WithFields(log.Fields{
			"id": hap.properties.Id,
		}).WithError(err).Error("Failed to write syslog fragment")
		// TODO Should we rollback on syslog error ?
		return ERR_SYSLOG, err
	}
	log.WithFields(hap.Context.Fields()).WithFields(log.Fields{
		"id":     hap.properties.Id,
		"content":  string(data.Conf.Syslog),
		"filename": hap.Paths.Syslog,
	}).Debug("Write syslog fragment")

	return SUCCESS, nil
}

// dumpConfiguration dumps the new configuration file with context for debugging purpose
func (hap *Haproxy) dumpConfiguration(filename string, newConf []byte, data *EventMessageWithConf) {
	f, err2 := os.Create(filename)
	defer f.Close()
	if err2 == nil {
		f.WriteString("================================================================\n")
		f.WriteString(fmt.Sprintf("application: %s\n", data.Header.Application))
		f.WriteString(fmt.Sprintf("platform: %s\n", data.Header.Platform))
		f.WriteString(fmt.Sprintf("correlationId: %s\n", data.Header.CorrelationId))
		f.WriteString("================================================================\n")
		f.Write(newConf)
		f.Sync()

		log.WithFields(hap.Context.Fields()).WithFields(log.Fields{
			"id":     hap.properties.Id,
			"filename": filename,
		}).Info("Dump configuration")
	}
}


// NewErrorPath gives a unique path the error file given the hap context
// It returns the full path to the file
func (hap *Haproxy) NewErrorPath() string {
	baseDir := hap.properties.HapHome + "/" + hap.Context.Application + "/errors"
	prefix := time.Now().Format("20060102150405")
	return baseDir + "/" + prefix + "_" + hap.Context.Application + hap.Context.Platform + ".log"
}

func (hap *Haproxy) NewDebugPath() string {
	baseDir := hap.properties.HapHome + "/" + hap.Context.Application + "/dump"
	prefix := time.Now().Format("20060102150405")
	return baseDir + "/" + prefix + "_" + hap.Context.Application + hap.Context.Platform + ".log"
}

// reload calls external shell script to reload haproxy
// It returns error if the reload fails
func (hap *Haproxy) reload(correlationId string) error {
	pid, err := ioutil.ReadFile(hap.Paths.Pid)
	if err != nil {
		log.WithField("pid path", string(hap.Paths.Pid)).Error("can't read pid file")
		return err
	}
	log.WithFields(log.Fields{"reloadScript":hap.HaproxyBinLink, "confPath":hap.Paths.Config, "pidPath":hap.Paths.Pid, "pid":string(pid)}).Debug("reload haproxy")
	output, err := hap.Command(hap.HaproxyBinLink, "-f", hap.Paths.Config, "-p", hap.Paths.Pid, "-sf", string(pid))

	if err == nil {
		log.WithFields(hap.Context.Fields()).WithFields(log.Fields{
			"id":         hap.properties.Id,
			"reloadScript": hap.HaproxyBinLink,
			"output":          string(output[:]),
		}).Debug("Reload succeeded")
	} else {
		log.WithFields(hap.Context.Fields()).WithField("output", string(output[:])).WithError(err).Error("Error reloading")

	}
	return err
}

// rollback reverts configuration files and call for reload
func (hap *Haproxy) rollback(correlationId string) error {
	if _, err := os.Stat(hap.Paths.Archive); os.IsNotExist(err) {
		return errors.New("No configuration file to rollback")
	}
	// TODO remove current hap.confPath() ?
	os.Rename(hap.Paths.Archive, hap.Paths.Config)
	hap.reload(correlationId)
	return nil
}

// createSkeleton creates the directory tree for a new haproxy context
func (configDir *ConfigDir) createSkeleton(context Context) error {
	for _, directory := range *configDir {
		err := createDirectory(context, directory)
		if err != nil {
			return err
		}
	}
	return nil
}


// updateSymlink create or update a symlink
func updateSymlink(context Context, oldname, newname string) error {
	newLink := true
	if _, err := os.Stat(newname); err == nil {
		os.Remove(newname)
		newLink = false
	}
	err := os.Symlink(oldname, newname)
	if err != nil {
		log.WithFields(context.Fields()).WithError(err).WithFields(log.Fields{
			"path": newname,
		}).Error("Symlink failed")
		return err
	}

	if newLink {
		log.WithFields(context.Fields()).WithFields(log.Fields{
			"path": newname,
		}).Info("Symlink created")
	}
	return nil
}

// createDirectory recursively creates directory if it doesn't exists
func createDirectory(context Context, dir string) error {
	if _, err := os.Stat(dir); os.IsNotExist(err) {
		err := os.MkdirAll(dir, 0755)
		if err != nil {
			log.WithError(err).WithFields(context.Fields()).WithFields(log.Fields{
				"dir": dir,
			}).Error("Failed to create")
			return err
		} else {
			log.WithFields(context.Fields()).WithFields(log.Fields{
				"dir": dir,
			}).Info("Directory created")
		}
	}
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
		log.WithError(err).WithFields(hap.Context.Fields()).WithFields(log.Fields{
			"dir": baseDir,
		}).Error("Failed to delete haproxy")
	} else {
		log.WithFields(hap.Context.Fields()).WithFields(log.Fields{
			"dir": baseDir,
		}).Info("HAproxy deleted")
	}

	return err
}

func (hap *Haproxy) Stop() error {
	reloadScript := hap.getReloadScript()
	output, err := hap.Command("sh", reloadScript, "stop")
	if err != nil {
		log.WithFields(hap.Context.Fields()).WithError(err).Error("Error stop")
	} else {
		log.WithFields(hap.Context.Fields()).WithFields(log.Fields{
			"reloadScript": reloadScript,
			"cmd":          string(output[:]),
		}).Debug("Stop succeeded")
	}
	return err
}

func (hap Haproxy) Fake() bool {
	return false
}