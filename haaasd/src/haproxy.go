package haaasd

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

func NewHaproxy(properties *Config, application string, platform string, version string) *Haproxy {
	if version == "" {
		version = "1.4.22"
	}
	return &Haproxy{
		Application: application,
		Platform:    platform,
		properties:  properties,
		Version:     version,
	}
}

type Haproxy struct {
	Application string
	Platform    string
	Version     string
	properties  *Config
	State       int
}

const (
	SUCCESS int = iota
	UNCHANGED int = iota
	ERR_SYSLOG int = iota
	ERR_CONF int = iota
	ERR_RELOAD int = iota
)

// ApplyConfiguration write the new configuration and reload
// A rollback is called on failure
func (hap *Haproxy) ApplyConfiguration(data *EventMessage) (int, error) {
	hap.createSkeleton()

	newConf := data.Conf
	path := hap.confPath()

	// Check conf diff
	oldConf, err := ioutil.ReadFile(path)

	if err != nil {
		log.WithField("path",path).Error("Cannot read old configuration")
		return ERR_CONF, err
	}

	if bytes.Equal(oldConf, newConf) {
		log.WithFields(log.Fields{
			"application": data.Application,
			"plateform":   data.Platform,
		}).Info("Ignore unchanged configuration")
		return UNCHANGED, nil
	}

	// Archive previous configuration
	archivePath := hap.confArchivePath()
	os.Rename(path, archivePath)
	log.WithField("archivePath", archivePath).Info("Old configuration saved")
	err = ioutil.WriteFile(path, newConf, 0644)
	if err != nil {
		return ERR_CONF, err
	}
	log.WithField("path", path).Info("New configuration written to %s", path)

	// Reload haproxy
	err = hap.reload()
	if err != nil {
		log.WithFields(log.Fields{
			"application": data.Application,
			"plateform":   data.Platform,
		}).WithError(err).Error("Reload failed")
		hap.dumpConfiguration(newConf, data)
		err = hap.rollback()
		return ERR_RELOAD, err
	}
	// Write syslog fragment
	fragmentPath := hap.syslogFragmentPath()
	err = ioutil.WriteFile(fragmentPath, data.SyslogFragment, 0644)
	if err != nil {
		log.WithFields(log.Fields{
			"application": data.Application,
			"plateform":   data.Platform,
		}).WithError(err).Error("Failed to write syslog fragment")
		// TODO Should we rollback on syslog error ?
		return ERR_SYSLOG, err
	}

	return SUCCESS, nil
}

// dumpConfiguration dumps the new configuration file with context for debugging purpose
func (hap *Haproxy) dumpConfiguration(newConf []byte, data *EventMessage) {
	errorFilename := hap.NewErrorPath()
	f, err2 := os.Create(errorFilename)
	defer f.Close()
	if err2 == nil {
		f.WriteString("================================================================\n")
		f.WriteString(fmt.Sprintf("application: %s\n", data.Application))
		f.WriteString(fmt.Sprintf("platform: %s\n", data.Platform))
		f.WriteString(fmt.Sprintf("correlationid: %s\n", data.Correlationid))
		f.WriteString("================================================================\n")
		f.Write(newConf)
		f.Sync()

		log.WithField("filename", errorFilename).Info("Invalid conf logged into %s")
	}
}

// confPath give the path of the configuration file given an application context
// It returns the absolute path to the file
func (hap *Haproxy) confPath() string {
	baseDir := hap.properties.HapHome + "/" + hap.Application + "/Config"
	os.MkdirAll(baseDir, 0755)
	return baseDir + "/hap" + hap.Application + hap.Platform + ".conf"
}

// confPath give the path of the archived configuration file given an application context
func (hap *Haproxy) confArchivePath() string {
	baseDir := hap.properties.HapHome + "/" + hap.Application + "/version-1"
	// It returns the absolute path to the file
	os.MkdirAll(baseDir, 0755)
	return baseDir + "/hap" + hap.Application + hap.Platform + ".conf"
}

// NewErrorPath gives a unique path the error file given the hap context
// It returns the full path to the file
func (hap *Haproxy) NewErrorPath() string {
	baseDir := hap.properties.HapHome + "/" + hap.Application + "/errors"
	os.MkdirAll(baseDir, 0755)
	prefix := time.Now().Format("20060102150405")
	return baseDir + "/" + prefix + "_" + hap.Application + hap.Platform + ".log"
}

// reload calls external shell script to reload haproxy
// It returns error if the reload fails
func (hap *Haproxy) reload() error {

	reloadScript := hap.getReloadScript()
	cmd, err := exec.Command("sh", reloadScript, "reload", "-y").Output()
	if err != nil {
		log.WithError(err).Error("Error reloading")
	}
	log.WithField("reloadScript", reloadScript).WithField("cmd", cmd).Debug("Reload succeeded")
	return err
}

// rollbac reverts configuration files and call for reload
func (hap *Haproxy) rollback() error {
	lastConf := hap.confArchivePath()
	if _, err := os.Stat(lastConf); os.IsNotExist(err) {
		return errors.New("No configuration file to rollback")
	}
	os.Rename(lastConf, hap.confPath())
	hap.reload()
	return nil
}

// createSkeleton creates the directory tree for a new haproxy context
func (hap *Haproxy) createSkeleton() error {
	baseDir := hap.properties.HapHome + "/" + hap.Application

	createDirectory(baseDir + "/Config")
	createDirectory(baseDir + "/logs/" + hap.Application + hap.Platform)
	createDirectory(baseDir + "/scripts")
	createDirectory(baseDir + "/version-1")

	updateSymlink(hap.getHapctlFilename(), hap.getReloadScript())
	updateSymlink(hap.getHapBinary(), baseDir + "/Config/haproxy")

	log.WithField("dir", baseDir).Info("Skeleton created")

	return nil
}

// confPath give the path of the configuration file given an application context
// It returns the absolute path to the file
func (hap *Haproxy) syslogFragmentPath() string {
	baseDir := hap.properties.HapHome + "/SYSLOG/Config/syslog.conf.d"
	os.MkdirAll(baseDir, 0755)
	return baseDir + "/syslog" + hap.Application + hap.Platform + ".conf"
}

// updateSymlink create or update a symlink
func updateSymlink(oldname string, newname string) {
	if _, err := os.Stat(newname); err == nil {
		os.Remove(newname)
	}
	err := os.Symlink(oldname, newname)
	if err != nil {
		log.WithError(err).WithField("path", newname).Error("Failed to create symlink")
	}
}

// createDirectory recursively creates directory if it doesn't exists
func createDirectory(dir string) {
	if _, err := os.Stat(dir); os.IsNotExist(err) {
		err := os.MkdirAll(dir, 0755)
		if err != nil {
			log.WithError(err).WithField("dir", dir).Error("Failed to create")
		} else {
			log.WithField("dir", dir).Println("Directory created")
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
	return fmt.Sprintf("%s/%s/scripts/hapctl%s%s", hap.properties.HapHome, hap.Application, hap.Application, hap.Platform)
}

// getHapBinary calculates the haproxy binary to use given the expected version
// It returns the full path to the haproxy binary
func (hap *Haproxy) getHapBinary() string {
	return fmt.Sprintf("/export/product/haproxy/product/%s/bin/haproxy", hap.Version)
}
