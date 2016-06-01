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
)

func NewHaproxy(role string, properties *Config, version string, context Context) *Haproxy {
	if version == "" {
		version = "1.4.22"
	}
	return &Haproxy{
		Role:       role,
		properties: properties,
		Version:    version,
		Context:    context,
	}
}

type Haproxy struct {
	Role       string
	Version    string
	properties *Config
	State      int
	Context    Context
}

const (
	SUCCESS    int = iota
	UNCHANGED  int = iota
	ERR_SYSLOG int = iota
	ERR_CONF   int = iota
	ERR_RELOAD int = iota
)

// ApplyConfiguration write the new configuration and reload
// A rollback is called on failure
func (hap *Haproxy) ApplyConfiguration(data *EventMessage) (int, error) {
	hap.createSkeleton(data.CorrelationId)

	newConf := data.Conf
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
	err = hap.reload(data.CorrelationId)
	if err != nil {
		log.WithFields(hap.Context.Fields()).WithFields(log.Fields{
			"role": hap.Role,
		}).WithError(err).Error("Reload failed")
		hap.dumpConfiguration(hap.NewErrorPath(), newConf, data)
		errRollback := hap.rollback(data.CorrelationId)
		if errRollback != nil {
			log.WithError(errRollback).Error("error in rollback in addition to error of the reload")
		} else {
			log.WithFields(hap.Context.Fields()).Debug("rollback done")
		}
		return ERR_RELOAD, err
	}
	// Write syslog fragment
	fragmentPath := hap.syslogFragmentPath()
	err = ioutil.WriteFile(fragmentPath, data.SyslogFragment, 0644)
	if err != nil {
		log.WithFields(hap.Context.Fields()).WithFields(log.Fields{
			"role": hap.Role,
		}).WithError(err).Error("Failed to write syslog fragment")
		// TODO Should we rollback on syslog error ?
		return ERR_SYSLOG, err
	}
	log.WithFields(hap.Context.Fields()).WithFields(log.Fields{
		"role":     hap.Role,
		"content":  string(data.SyslogFragment),
		"filename": fragmentPath,
	}).Debug("Write syslog fragment")

	return SUCCESS, nil
}

// dumpConfiguration dumps the new configuration file with context for debugging purpose
func (hap *Haproxy) dumpConfiguration(filename string, newConf []byte, data *EventMessage) {
	f, err2 := os.Create(filename)
	defer f.Close()
	if err2 == nil {
		f.WriteString("================================================================\n")
		f.WriteString(fmt.Sprintf("application: %s\n", data.Application))
		f.WriteString(fmt.Sprintf("platform: %s\n", data.Platform))
		f.WriteString(fmt.Sprintf("correlationId: %s\n", data.CorrelationId))
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
		log.WithFields(hap.Context.Fields()).WithError(err).Error("Error reloading")
	} else {
		log.WithFields(hap.Context.Fields()).WithFields(log.Fields{
			"role":         hap.Role,
			"reloadScript": reloadScript,
			"cmd":          string(output[:]),
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
func (hap *Haproxy) createSkeleton(correlationId string) error {
	baseDir := hap.properties.HapHome + "/" + hap.Context.Application

	createDirectory(hap.Context, correlationId, baseDir+"/Config")
	createDirectory(hap.Context, correlationId, baseDir+"/logs/"+hap.Context.Application+hap.Context.Platform)
	createDirectory(hap.Context, correlationId, baseDir+"/scripts")
	createDirectory(hap.Context, correlationId, baseDir+"/version-1")
	createDirectory(hap.Context, correlationId, baseDir+"/errors")
	createDirectory(hap.Context, correlationId, baseDir+"/dump")

	updateSymlink(hap.Context, correlationId, hap.getHapctlFilename(), hap.getReloadScript())
	updateSymlink(hap.Context, correlationId, hap.getHapBinary(), baseDir+"/Config/haproxy")

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

// getHapBinary calculates the haproxy binary to use given the expected version
// It returns the full path to the haproxy binary
func (hap *Haproxy) getHapBinary() string {
	return fmt.Sprintf("/export/product/haproxy/product/%s/bin/haproxy", hap.Version)
}

func (hap *Haproxy) Delete() {
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
