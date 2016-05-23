package sidekick

import (
//	"fmt"
//	"io/ioutil"
//	"log"
//	"net/http"
)

type Daemon struct {
	Properties *Config
}

func (daemon *Daemon) IsMaster() (bool, error) {
	return (daemon.Properties.Status == "master"), nil

	//	resp, err := http.Get(fmt.Sprintf("http://%s:%d/real-ip", daemon.Properties.Vip, daemon.Properties.Port))
	//	if err != nil {
	//		log.Fatal(err)
	//	}
	//	defer resp.Body.Close()
	//	body, err := ioutil.ReadAll(resp.Body)
	//	log.Printf("master ip: %s", body)
	//	return string(body) == daemon.Properties.IpAddr, nil
}

func (daemon *Daemon) IsSlave() (bool, error) {
	return (daemon.Properties.Status == "slave"), nil
	//	isMaster, err := daemon.IsMaster()
	//	return !isMaster, err
}

func (daemon *Daemon) Is(target string) (bool, error) {
	return (daemon.Properties.Status == target), nil
}

func NewDaemon(properties *Config) *Daemon {
	return &Daemon{
		Properties: properties,
	}
}
