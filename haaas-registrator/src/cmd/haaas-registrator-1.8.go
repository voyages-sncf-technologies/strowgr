package main

import (
	registrator "../."
	log "github.com/Sirupsen/logrus"
	"flag"
	"os"
	"strings"
	"github.com/samalba/dockerclient"
	"fmt"
	"os/signal"
	"syscall"
)

var (
	version bool
	adminUrl string
	address string
	debug bool
	client *dockerclient.DockerClient
)

const (
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
				if getMetadata(info.Config, APPLICATION_LABEL) == "" {
					log.WithField("container", info.Name).WithField("key", APPLICATION_LABEL).Debug("Metadata is missing")
					return
				}

				if getMetadata(info.Config, PLATFORM_LABEL) == "" {
					log.WithField("container", info.Name).WithField("key", PLATFORM_LABEL).Debug("Metadata is missing")
					return
				}

				for exposedPort, _ := range info.Config.ExposedPorts {
					private_port := strings.Replace(exposedPort, "/", "_", -1)
					public_ports := info.NetworkSettings.Ports[exposedPort]
					if public_ports == nil || len(public_ports) == 0 {
						log.WithField("private_port", private_port).Debug("Port not published")
						continue
					}

					serviceLabel := fmt.Sprintf(SERVICE_NAME_LABEL, private_port)
					if getMetadata(info.Config, serviceLabel) == "" {
						log.WithField("container", info.Name).WithField("label", serviceLabel).Debug("Label is missing")
						continue
					}
					public_port := public_ports[0].HostPort
					log.WithField("port", private_port).Debug("Analyze container")

					id := strings.Replace(address, ".", "_", -1) + strings.Replace(info.Name, "/", "_", -1) + "_" + public_port
					instance := registrator.NewInstance();
					instance.Id = id
					instance.App = getMetadata(info.Config, APPLICATION_LABEL)
					instance.Platform = getMetadata(info.Config, PLATFORM_LABEL)
					instance.Service = getMetadata(info.Config, serviceLabel)
					instance.Port = public_port
					instance.Ip = address
					instance.Hostname = id
					instance.Register(adminUrl)

				}
			}
		}
	}
}

func getMetadata(config *dockerclient.ContainerConfig, key string) string {
	if config.Labels[key] != "" {
		return config.Labels[key]
	}else {
		return getEnv(config.Env, key)
	}
}

func getEnv(haystack []string, needle string) string {
	for index := range haystack {
		res := strings.Split(haystack[index], "=")
		if res[0] == needle {
			return res[1]
		}
	}
	return ""
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

	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, os.Interrupt, syscall.SIGTERM, syscall.SIGQUIT)
	select {
	case signal := <-sigChan:
		log.Printf("Got signal: %v\n", signal)
	}

}

