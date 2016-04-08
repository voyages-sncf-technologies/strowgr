package main

import (
	"github.com/docker/engine-api/client"
	"github.com/docker/engine-api/types"
	eventtypes "github.com/docker/engine-api/types/events"
	"github.com/docker/engine-api/types/filters"
	events "github.com/vdemeester/docker-events"
	"golang.org/x/net/context"
	log "github.com/Sirupsen/logrus"
	"flag"
	"os"
	"net/http"
	"fmt"
	"io/ioutil"
	"encoding/json"
	"bytes"
	"strconv"
"strings"
)

var (
	version bool
	adminUrl string
	address string
	debug bool
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
		println(VERSION)
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
				if info.Config.Labels["APPLICATION"] == "" {
					log.WithField("container", info.Name).WithField("label", "APPLICATION").Debug("Label is missing")
					return
				}

				if info.Config.Labels["PLATFORM"] == "" {
					log.WithField("container", info.Name).WithField("label", "PLATFORM").Debug("Label is missing")
					return
				}

				for exposedPort, _ := range info.Config.ExposedPorts {
					private_port := strconv.Itoa(exposedPort.Int())
					public_ports := info.NetworkSettings.Ports[exposedPort]
					if public_ports == nil || len(public_ports) == 0 {
						log.WithField("private_port", private_port).Debug("Port not published")
						continue
					}

					if info.Config.Labels["SERVICE_" + private_port + "_NAME"] == "" {
						log.WithField("container", info.Name).WithField("label", "SERVICE_" + private_port + "_NAME").Debug("Label is missing")
						continue
					}
					public_port := public_ports[0].HostPort
					log.WithField("port", private_port).Debug("Analyze container")

					id := strings.Replace(address, ".", "_", -1) + "_" + public_port
					instance := NewInstance();
					instance.Id =  id
					instance.App = info.Config.Labels["APPLICATION"]
					instance.Platform = info.Config.Labels["PLATFORM"]
					instance.Service = info.Config.Labels["SERVICE_" + private_port + "_NAME"]
					instance.Port = public_port
					instance.Ip = address
					instance.Hostname = id
					register_instance(instance)

					//for label, value := range info.Config.Labels{
					//	if strings.HasPrefix(label,"SERVICE_" + port + "_CONTEXT"){
					//		key := label[len("SERVICE_" + port + "_CONTEXT"),len(label)-1]
					//
					//	}
					//}
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
		// Do something
	}
}

func NewInstance() *Instance {
	return &Instance{
		Context:make(map[string]string),
		ContextOverride:make(map[string]string),
	}
}

func register_instance(instance *Instance) {
	log.WithFields(log.Fields{
		"id": instance.Id,
		"application": instance.App,
		"platform": instance.Platform,
	}).Info("Register")
	var url = fmt.Sprintf("%s/api/entrypoint/%s/%s/backend/%s/register-server", adminUrl, instance.App, instance.Platform, instance.Service)
	json, _ := json.Marshal(instance)
	req, err := http.NewRequest("POST", url, bytes.NewBuffer(json))
	req.Header.Set("Content-Type", "application/json")

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		log.WithError(err).WithField("url", url).WithField("json", string(json)).Error("Error requesting")
		return
	}
	defer resp.Body.Close()

	fmt.Println("response Status:", resp.Status)
	fmt.Println("response Headers:", resp.Header)
	body, _ := ioutil.ReadAll(resp.Body)
	fmt.Println("response Body:", string(body))
}

type Instance struct {
	Id              string `json:"id"`
	Hostname        string `json:"hostname"`
	Ip              string `json:"ip"`
	Port            string `json:"port"`
	App             string `json:"-"`
	Platform        string `json:"-"`
	Service         string `json:"-"`
	Context         map[string]string `json:"context"`
	ContextOverride map[string]string `json:"contextOverride"`
}
