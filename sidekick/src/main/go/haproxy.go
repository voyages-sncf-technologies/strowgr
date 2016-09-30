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
	"os/exec"
	"time"
	"strings"
)

func NewHaproxy(role string, properties *Config, context Context) *Haproxy {

	return &Haproxy{
		Role:       role,
		properties: properties,
		Versions:    properties.HapVersions,
		Context:    context,
	}
}

type Haproxy struct {
	Role       string
	Versions   []string
	properties *Config
	State      int
	Context    Context
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
	for _, version := range hap.Versions {
		present = (version == data.Conf.Version)
	}
	// validate that received haproxy configuration contains a managed version of haproxy
	if data.Conf.Version == "" || !present {
		log.WithFields(log.Fields{
			"given haproxy version":data.Conf.Version,
			"managed versions by sidekick": strings.Join(hap.Versions, ",")}).Error("received configuration hasn't haproxy version or one which has not been configured in this sidekick instance")
		return ERR_CONF, errors.New("received configuration hasn't haproxy version or one which has not been configured in this sidekick instance")
	}

	hap.createSkeleton(data.Header.CorrelationId, data.Conf)

	newConf := data.Conf.Haproxy
	path := hap.confPath()

	// Check conf diff
	oldConf, err := ioutil.ReadFile(path)
	if log.GetLevel() == log.DebugLevel {
		hap.dumpConfiguration(hap.NewDebugPath(), newConf, data)
	}
	if bytes.Equal(oldConf, newConf) {
		log.WithFields(hap.Context.Fields()).WithFields(
			log.Fields{"role": hap.Role}).Debug("Unchanged configuration")
		return UNCHANGED, nil
	}

	// Archive previous configuration
	archivePath := hap.confArchivePath()
	os.Rename(path, archivePath)
	log.WithFields(hap.Context.Fields()).WithFields(
		log.Fields{
			"role":        hap.Role,
			"archivePath": archivePath,
		}).Info("Old configuration saved")
	err = ioutil.WriteFile(path, newConf, 0644)
	if err != nil {
		return ERR_CONF, err
	}

	log.WithFields(hap.Context.Fields()).WithFields(log.Fields{
		"role": hap.Role,
		"path": path,
	}).Info("New configuration written")

	// Reload haproxy
	err = hap.reload(data.Header.CorrelationId)
	if err != nil {
		log.WithFields(hap.Context.Fields()).WithFields(log.Fields{
			"role": hap.Role,
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
	fragmentPath := hap.syslogFragmentPath()
	err = ioutil.WriteFile(fragmentPath, data.Conf.Syslog, 0644)
	if err != nil {
		log.WithFields(hap.Context.Fields()).WithFields(log.Fields{
			"role": hap.Role,
		}).WithError(err).Error("Failed to write syslog fragment")
		// TODO Should we rollback on syslog error ?
		return ERR_SYSLOG, err
	}
	log.WithFields(hap.Context.Fields()).WithFields(log.Fields{
		"role":     hap.Role,
		"content":  string(data.Conf.Syslog),
		"filename": fragmentPath,
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
			"role":     hap.Role,
			"filename": filename,
		}).Info("Dump configuration")
	}
}

// confPath give the path of the configuration file given an application context
// It returns the absolute path to the file
func (hap *Haproxy) confPath() string {
	baseDir := hap.properties.HapHome + "/" + hap.Context.Application + "/Config"
	return baseDir + "/hap" + hap.Context.Application + hap.Context.Platform + ".conf"
}

// confPath give the path of the archived configuration file given an application context
func (hap *Haproxy) confArchivePath() string {
	baseDir := hap.properties.HapHome + "/" + hap.Context.Application + "/version-1"
	return baseDir + "/hap" + hap.Context.Application + hap.Context.Platform + ".conf"
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
	reloadScript := hap.getReloadScript()
	output, err := exec.Command("sh", reloadScript, "reload", "-y").Output()
	if err != nil {
		log.WithFields(hap.Context.Fields()).WithField("output", string(output[:])).WithError(err).Error("Error reloading")
	} else {
		log.WithFields(hap.Context.Fields()).WithFields(log.Fields{
			"role":         hap.Role,
			"reloadScript": reloadScript,
			"output":          string(output[:]),
		}).Debug("Reload succeeded")
	}
	return err
}

// rollback reverts configuration files and call for reload
func (hap *Haproxy) rollback(correlationId string) error {
	lastConf := hap.confArchivePath()
	if _, err := os.Stat(lastConf); os.IsNotExist(err) {
		return errors.New("No configuration file to rollback")
	}
	// TODO remove current hap.confPath() ?
	os.Rename(lastConf, hap.confPath())
	hap.reload(correlationId)
	return nil
}

// createSkeleton creates the directory tree for a new haproxy context
func (hap *Haproxy) createSkeleton(correlationId string, conf Conf) error {
	baseDir := hap.properties.HapHome + "/" + hap.Context.Application

	createDirectory(hap.Context, correlationId, baseDir + "/Config")
	createDirectory(hap.Context, correlationId, baseDir + "/logs/" + hap.Context.Application + hap.Context.Platform)
	createDirectory(hap.Context, correlationId, baseDir + "/scripts")
	createDirectory(hap.Context, correlationId, baseDir + "/version-1")
	createDirectory(hap.Context, correlationId, baseDir + "/errors")
	createDirectory(hap.Context, correlationId, baseDir + "/dump")

	updateSymlink(hap.Context, correlationId, hap.getHapctlFilename(), hap.getReloadScript())
	updateSymlink(hap.Context, correlationId, hap.getHapBinary(conf), baseDir + "/Config/haproxy")

	return nil
}

// confPath give the path of the configuration file given an application context
// It returns the absolute path to the file
func (hap *Haproxy) syslogFragmentPath() string {
	baseDir := hap.properties.HapHome + "/SYSLOG/Config/syslog.conf.d"
	os.MkdirAll(baseDir, 0755)
	return baseDir + "/syslog" + hap.Context.Application + hap.Context.Platform + ".conf"
}

// updateSymlink create or update a symlink
func updateSymlink(context Context, correlationId, oldname, newname string) {
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
	}

	if newLink {
		log.WithFields(context.Fields()).WithFields(log.Fields{
			"path": newname,
		}).Info("Symlink created")
	}
}

// createDirectory recursively creates directory if it doesn't exists
func createDirectory(context Context, correlationId string, dir string) {
	if _, err := os.Stat(dir); os.IsNotExist(err) {
		err := os.MkdirAll(dir, 0755)
		if err != nil {
			log.WithError(err).WithFields(context.Fields()).WithFields(log.Fields{
				"dir": dir,
			}).Error("Failed to create")
		} else {
			log.WithFields(context.Fields()).WithFields(log.Fields{
				"dir": dir,
			}).Info("Directory created")
		}
	}
}

// getHapctlFilename return the path to the vsc hapctl shell script
// This script is provided
func (hap *Haproxy) getHapctlFilename() string {
	return "/HOME/uxwadm/scripts/hapctl_unif"
}

// getReloadScript calculates reload script path given the hap context
// It returns the full script path
func (hap *Haproxy) getReloadScript() string {
	return fmt.Sprintf("%s/%s/scripts/hapctl%s%s", hap.properties.HapHome, hap.Context.Application, hap.Context.Application, hap.Context.Platform)
}

// getHapBinary calculates the haproxy binary to use with given version
// It returns the full path to the haproxy binary
func (hap *Haproxy) getHapBinary(conf Conf) string {
	return fmt.Sprintf("/export/product/haproxy/product/%s/bin/haproxy", conf.Version)
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
	output, err := exec.Command("sh", reloadScript, "stop").Output()
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