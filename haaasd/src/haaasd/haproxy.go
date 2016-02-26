package haaasd

import (
	"fmt"
	"io/ioutil"
	"log"
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
		Version: version,
	}
}

type Haproxy struct {
	Application string
	Platform    string
	Version     string
	properties  *Config
	State       int
}

func (hap *Haproxy) ApplyConfiguration(data *EventMessage) error {
	hap.createSkeleton()

	newConf := data.Conf
	// /appl/hapadm/DTC/version-1/
	path := hap.confPath()
	archivePath := hap.confArchivePath()
	os.Rename(path, archivePath)
	log.Printf("Old configurqtion saved to %s", archivePath)
	err := ioutil.WriteFile(path, newConf, 0644)
	if err != nil {
		return err
	}
	log.Printf("New configuration written to %s", path)
	err = hap.reload(data)
	if err != nil {
		log.Printf("can't apply reload of %s-%s. Error: %s", data.Application, data.Platform, err)
		ioutil.WriteFile(path, newConf, 0644)

		errorFilename := hap.NewErrorPath()
		f, err2 := os.Create(errorFilename)
		if err2 == nil {
			f.WriteString("================================================================\n")
			f.WriteString(fmt.Sprintf("application: %s\n", data.Application))
			f.WriteString(fmt.Sprintf("platform: %s\n", data.Platform))
			f.WriteString(fmt.Sprintf("correlationid: %s\n", data.Correlationid))
			f.WriteString("================================================================\n")
			f.Write(newConf)
			f.Sync()
			log.Printf("Invalid conf logged into %s", errorFilename)
			return err
		}
		defer f.Close()
		//		err = hap.rollback()
	}

	return err
}

func (hap *Haproxy) confPath() string {
	baseDir := hap.properties.HapHome + "/" + hap.Application + "/Config"
	os.MkdirAll(baseDir, 0755)
	return baseDir + "/hap" + hap.Application + hap.Platform + ".conf"
}

func (hap *Haproxy) confArchivePath() string {
	baseDir := hap.properties.HapHome + "/" + hap.Application + "/version-1"
	os.MkdirAll(baseDir, 0755)
	return baseDir + "/hap" + hap.Application + hap.Platform + ".conf"
}

func (hap *Haproxy) NewErrorPath() string {
	baseDir := hap.properties.HapHome + "/" + hap.Application + "/errors"
	os.MkdirAll(baseDir, 0755)
	prefix := time.Now().Format("20060102150405")
	return baseDir + "/" + prefix + "_" + hap.Application + hap.Platform + ".log"
}

func (hap *Haproxy) reload(data *EventMessage) error {

	reloadScript := hap.getReloadScript()
	cmd, err := exec.Command("sh", reloadScript, "reload").Output()
	if err != nil {
		log.Printf("Error reloading %s", err)
	}
	log.Printf("result %s: %s", reloadScript, cmd)
	return err
}

func (hap *Haproxy) rollback() error {
	return nil
}

func (hap *Haproxy) createSkeleton() error {
	baseDir := hap.properties.HapHome + "/" + hap.Application

	createDirectory(baseDir + "/Config")
	createDirectory(baseDir + "/logs/" + hap.Application + hap.Platform)
	createDirectory(baseDir + "/scripts")
	createDirectory(baseDir + "/version-1")

	updateSymlink(hap.getHapctlFilename(), hap.getReloadScript())
	updateSymlink(hap.getHapBinary(), baseDir + "/Config/haproxy")

	log.Printf("%s created", baseDir)

	return nil
}

func updateSymlink(oldname string, newname string) {
	if _, err := os.Stat(newname); err == nil {
		os.Remove(newname)
	}
	err := os.Symlink(oldname, newname)
	if err != nil {
		log.Println("Failed to create symlink ", newname, err)
	}
}

func createDirectory(dir string) {
	if _, err := os.Stat(dir); os.IsNotExist(err) {
		err := os.MkdirAll(dir, 0755)
		if err != nil {
			log.Print("Failed to create", dir, err)
		}else {
			log.Println(dir, " created")
		}
	}
}

func (hap *Haproxy) getHapctlFilename() string {
	return "/HOME/uxwadm/scripts/hapctl_unif"
}

func (hap *Haproxy) getReloadScript() string {
	return fmt.Sprintf("/%s/%s/scripts/hapctl%s%s", hap.properties.HapHome, hap.Application, hap.Application, hap.Platform)
}

func (hap *Haproxy) getHapBinary() string {
	return fmt.Sprintf("/export/product/haproxy/product/%s/bin/haproxy", hap.Version)
}

