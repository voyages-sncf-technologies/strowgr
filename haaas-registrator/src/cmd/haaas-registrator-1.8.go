package main

import (
	registrator "../."
	log "github.com/Sirupsen/logrus"
	"flag"
	"os"
	"strings"
	"github.com/samalba/dockerclient"
	"fmt"
)

var (
	version bool
	adminUrl string
	address string
	debug bool
	client *dockerclient.DockerClient
)

const(
	APPLICATION_LABEL = "application.name"
	PLATFORM_LABEL = "platform.name"
	SERVICE_NAME_LABEL = "service.%s.name"
)

func init() {
	log.SetFormatter(new(log.TextFormatter))
}

func eventCallback(event *dockerclient.Event, ec chan error, args ...interface{}) {
	if event.Status == "start" {
		info, err := client.InspectContainer(event.ID)
		if err != nil {
			log.WithError(err).WithField("id", event.ID).Error("Cannot inspect container")
			return
		} else {

			log.WithField("info", info).Debug("Inspect container")
			if info.Config == nil || info.Config.ExposedPorts == nil {
				log.WithField("container", info.Name).Debug("No exposed ports")
			}else {
				if info.Config.Labels[APPLICATION_LABEL] == "" {
					log.WithField("container", info.Name).WithField("label", APPLICATION_LABEL).Debug("Label is missing")
					return
				}

				if info.Config.Labels[PLATFORM_LABEL] == "" {
					log.WithField("container", info.Name).WithField("label", PLATFORM_LABEL).Debug("Label is missing")
					return
				}

				for exposedPort, _ := range info.Config.ExposedPorts {
					private_port := exposedPort
					public_ports := info.NetworkSettings.Ports[exposedPort]
					if public_ports == nil || len(public_ports) == 0 {
						log.WithField("private_port", private_port).Debug("Port not published")
						continue
					}

					serviceLabel := fmt.Sprintf(SERVICE_NAME_LABEL,private_port)
					if info.Config.Labels[serviceLabel] == "" {
						log.WithField("container", info.Name).WithField("label",serviceLabel).Debug("Label is missing")
						continue
					}
					public_port := public_ports[0].HostPort
					log.WithField("port", private_port).Debug("Analyze container")

					id := strings.Replace(address, ".", "_", -1) + strings.Replace(info.Name, "/", "_", -1) + "_" + public_port
					instance := registrator.NewInstance();
					instance.Id = id
					instance.App = info.Config.Labels[APPLICATION_LABEL]
					instance.Platform = info.Config.Labels[PLATFORM_LABEL]
					instance.Service = info.Config.Labels[serviceLabel]
					instance.Port = public_port
					instance.Ip = address
					instance.Hostname = id
					instance.Register(adminUrl)

				}
			}
		}
	}

}

func main() {
	flag.BoolVar(&debug, "verbose", false, "debug mode")
	flag.BoolVar(&version, "version", false, "Show version")
	flag.StringVar(&adminUrl, "url", "", "Admin url")
	flag.StringVar(&address, "address", "", "Ip address")
	flag.Parse()

	if (version) {
		println(registrator.VERSION)
		os.Exit(0)
	}

	if (debug) {
		log.SetLevel(log.DebugLevel)
	}
	docker, err := dockerclient.NewDockerClient("unix:///var/run/docker.sock", nil)
	if err != nil {
		log.WithError(err).Fatal("Unable to start client")
	}

	client = docker
	log.Info("Starting")
	client.StartMonitorEvents(eventCallback, nil)

	select{}

}

