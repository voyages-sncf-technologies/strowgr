package haaas

import (
	"io/ioutil"
	"log"
	"os"
	"fmt"
	"os/exec"
)

func NewHaproxy(properties *Config, application string, platform string) *Haproxy {
	return &Haproxy{
		Application: application,
		Platform: platform,
		properties: properties,
	}
}

type Haproxy struct {
	Application string
	Platform    string
	properties  *Config
}

func (hap *Haproxy) ApplyConfiguration(data EventMessage) (error) {
	newConf := data.Conf
	// /appl/hapadm/DTC/version-1/
	path := hap.confPath()
	archivePath := hap.confArchivePath()
	os.Rename(path, archivePath)
	log.Printf("Old configurqtion saved to %s",archivePath)
	err := ioutil.WriteFile(path, newConf, 0644)
	if err != nil {
		return err
	}
	log.Printf("New configuration written to %s" , path )
	err = hap.reload(data)
	if err != nil {
		log.Fatal("can't apply reconfiguration of %+v. Error: %s", data, err)
		err = hap.rollback()
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

func (hap *Haproxy) reload(data EventMessage) (error) {

	cmd, err := exec.Command("sh", fmt.Sprintf("%s/%s/RELOAD", hap.properties.HapHome, data.Application)).Output()
	if err != nil {
		log.Fatal("Error reloading :%s", err)
	}
	log.Printf("result of %s/%s/RELOAD: %s", hap.properties.HapHome, data.Application, cmd)
	return err
}

func (hap *Haproxy) rollback() (error) {
	return nil
}