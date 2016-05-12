package main

import (
	"haaasregistrator"
	"github.com/docker/engine-api/client"
	"github.com/docker/engine-api/types"
	"github.com/docker/engine-api/types/container"
	eventtypes "github.com/docker/engine-api/types/events"
	"github.com/docker/engine-api/types/filters"
	events "github.com/vdemeester/docker-events"
	"golang.org/x/net/context"
	log "github.com/Sirupsen/logrus"
	"flag"
	"os"
	"strings"
	"fmt"
)

var (
	version bool
	adminUrl string
	address string
	debug bool
)

const (
	APPLICATION_LABEL = "application.name"
	PLATFORM_LABEL = "platform.name"
	SERVICE_NAME_LABEL = "service.%s.name"
)

func init() {
	log.SetFormatter(new(log.TextFormatter))
}

func main() {
	flag.BoolVar(&debug, "verbose", false, "debug mode")
	flag.BoolVar(&version, "version", false, "Show version")
	flag.StringVar(&adminUrl, "url", "", "Admin url")
	flag.StringVar(&address, "address", "", "Ip address")
	flag.Parse()

	if (version) {
		println(haaasregistrator.VERSION)
		os.Exit(0)
	}

	if (debug) {
		log.SetLevel(log.DebugLevel)
	}

	cli, err := client.NewEnvClient()
	cli.Info(context.Background())

	if err != nil {
		log.WithError(err).Fatal("Unable to start client")
	}

	// Setup the event handler
	eventHandler := events.NewHandler(events.ByAction)
	eventHandler.Handle("start", func(m eventtypes.Message) {

		info, err := cli.ContainerInspect(context.Background(), m.ID)
		log.WithField("info", info).Debug("Inspect container")
		if err != nil {
			log.WithError(err).WithField("containerId", m.ID).Error("Cannot register instance")
		}else {
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
					private_port := strings.Replace(exposedPort.Port(), "/", "_", -1)
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
					instance := haaasregistrator.NewInstance();
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
	})

	stoppedOrDead := func(m eventtypes.Message) {
		log.WithField("type", "remove").Info(m.From)
	}
	eventHandler.Handle("die", stoppedOrDead)
	eventHandler.Handle("stop", stoppedOrDead)

	// Filter the events we wams so receive
	filters := filters.NewArgs()
	filters.Add("type", "container")
	options := types.EventsOptions{
		Filters: filters,
	}

	log.Info("Starting")
	errChan := events.MonitorWithHandler(context.Background(), cli, options, eventHandler)

	if err := <-errChan; err != nil {
		log.WithError(err).Error("Error")
	}
}

func getMetadata(config *container.Config, key string) string {
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