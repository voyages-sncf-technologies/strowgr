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
package main

import (
	".."
	"bytes"
	"encoding/json"
	"flag"
	"fmt"
	"github.com/BurntSushi/toml"
	log "github.com/Sirupsen/logrus"
	"github.com/bitly/go-nsq"
	"net/http"
	"os"
	"os/signal"
	"sync"
	"syscall"
	"time"
)

var (
	ip = flag.String("ip", "4.3.2.1", "Node ip address")
	configFile = flag.String("config", "sidekick.conf", "Configuration file")
	version = flag.Bool("version", false, "Print current version")
	verbose = flag.Bool("verbose", false, "Log in verbose mode")
	fake = flag.String("fake", "yesman", "Force response without reload for testing purpose. 'yesman': always say ok, 'drunk': random status/errors for entrypoint updates. Just for test purpose.")
	config = nsq.NewConfig()
	properties       *sidekick.Config
	daemon           *sidekick.Daemon
	producer         *nsq.Producer
	syslog           *sidekick.Syslog
	reloadChan = make(chan sidekick.ReloadEvent)
	deleteChan = make(chan sidekick.ReloadEvent)
	lastSyslogReload = time.Now()
	haFactory *sidekick.LoadbalancerFactory
)

func main() {
	log.SetFormatter(&log.TextFormatter{})
	flag.Parse()

	if *version {
		println(sidekick.VERSION)
		os.Exit(0)
	}

	if *verbose {
		log.SetLevel(log.DebugLevel)
		log.WithField("loglevel", "debug").Info("Change loglevel")
	}

	loadProperties()

	haFactory = sidekick.NewLoadbalancerFactory()
	haFactory.Fake = *fake
	haFactory.Properties = properties

	daemon = sidekick.NewDaemon(properties)
	syslog = sidekick.NewSyslog(properties)
	syslog.Init()
	log.WithFields(log.Fields{
		"status": properties.Status,
		"id":     properties.NodeId(),
	}).Info("Starting sidekick")

	producer, _ = nsq.NewProducer(properties.ProducerAddr, config)

	createTopicsAndChannels()
	time.Sleep(1 * time.Second)

	var wg sync.WaitGroup
	// Start http API
	restApi := sidekick.NewRestApi(properties)
	go func() {
		defer wg.Done()
		wg.Add(1)
		err := restApi.Start()
		if err != nil {
			log.Fatal("Cannot start api")
		}
	}()

	// Start delete consumer
	go func() {
		defer wg.Done()
		wg.Add(1)
		consumer, _ := nsq.NewConsumer(fmt.Sprintf("delete_requested_%s", properties.ClusterId), fmt.Sprintf("%s", properties.NodeId()), config)
		consumer.AddHandler(nsq.HandlerFunc(onDeleteRequested))
		err := consumer.ConnectToNSQLookupd(properties.LookupdAddr)
		if err != nil {
			log.Panic("Could not connect")
		}
	}()

	// Start slave consumer
	go func() {
		defer wg.Done()
		wg.Add(1)
		consumer, _ := nsq.NewConsumer(fmt.Sprintf("commit_requested_%s", properties.ClusterId), fmt.Sprintf("%s", properties.NodeId()), config)
		consumer.AddHandler(nsq.HandlerFunc(onCommitRequested))
		err := consumer.ConnectToNSQLookupd(properties.LookupdAddr)
		if err != nil {
			log.Panic("Could not connect")
		}
	}()

	// Start master consumer
	go func() {
		defer wg.Done()
		wg.Add(1)
		consumer, _ := nsq.NewConsumer(fmt.Sprintf("commit_slave_completed_%s", properties.ClusterId), fmt.Sprintf("%s", properties.NodeId()), config)
		consumer.AddHandler(nsq.HandlerFunc(onCommitSlaveRequested))
		err := consumer.ConnectToNSQLookupd(properties.LookupdAddr)
		if err != nil {
			log.Panic("Could not connect")
		}
	}()

	// Start complete consumer
	go func() {
		defer wg.Done()
		wg.Add(1)
		consumer, _ := nsq.NewConsumer(fmt.Sprintf("commit_completed_%s", properties.ClusterId), fmt.Sprintf("%s", properties.NodeId()), config)
		consumer.AddHandler(nsq.HandlerFunc(onCommitCompleted))
		err := consumer.ConnectToNSQLookupd(properties.LookupdAddr)
		if err != nil {
			log.Panic("Could not connect")
		}
	}()

	// Start reload pipeline
	stopChan := make(chan interface{}, 1)
	go func() {
		defer wg.Done()
		wg.Add(1)
		for {
			select {
			case reload := <-reloadChan:
				reload.Execute()
			case delete := <-deleteChan:
				delete.Execute()
			case <-stopChan:
				return
			}
		}
	}()

	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, os.Interrupt, syscall.SIGTERM, syscall.SIGQUIT)
	select {
	case signal := <-sigChan:
		log.Printf("Got signal: %v\n", signal)
	}
	stopChan <- true
	restApi.Stop()

	log.Printf("Waiting on server to stop\n")
	wg.Wait()
}

func createTopicsAndChannels() {
	// Create required topics
	topics := []string{"commit_slave_completed", "commit_completed", "commit_failed"}
	channels := []string{"slave", "master"}
	topicChan := make(chan string, len(topics))

	// fill the channel
	for i := range topics {
		topicChan <- topics[i]
	}

	for len(topicChan) > 0 {
		// create the topic
		topic := <-topicChan
		log.WithField("topic", topic).WithField("clusterId", properties.ClusterId).Info("Creating topic")
		url := fmt.Sprintf("%s/topic/create?topic=%s_%s", properties.ProducerRestAddr, topic, properties.ClusterId)
		resp, err := http.PostForm(url, nil)
		if err != nil || resp.StatusCode != 200 {
			log.WithField("topic", topic).WithField("clusterid", properties.ClusterId).WithError(err).Error("topic can't be created")
			// retry to create this topic
			topicChan <- topic
			continue
		} else {
			log.WithField("topic", topic).WithField("clusterId", properties.ClusterId).Debug("topic created")
		}

		// create the channels of the topics
		for _, channel := range channels {
			log.WithField("channel", channel).Info("Creating channel")
			url := fmt.Sprintf("%s/channel/create?topic=%s_%s&channel=%s-%s", properties.ProducerRestAddr, topic, properties.ClusterId, properties.ClusterId, channel)
			resp, err := http.PostForm(url, nil)
			if err != nil || resp.StatusCode != 200 {
				// retry all for this topic if channel creation failed
				topicChan <- topic
				continue
			}
		}

		log.WithField("topic", topic).Info("Topic created")
	}
}

// loadProperties load properties file
func loadProperties() {
	properties = sidekick.DefaultConfig()
	if _, err := toml.DecodeFile(*configFile, properties); err != nil {
		log.Fatal(err)
		os.Exit(1)
	}
	properties.IpAddr = *ip
	len := len(properties.HapHome)
	if properties.HapHome[len - 1] == '/' {
		properties.HapHome = properties.HapHome[:len - 1]
	}
}

func filteredHandler(event string, message *nsq.Message, target string, f sidekick.HandlerFunc) error {
	defer message.Finish()
	var match bool
	if target == "any" {
		match = true
	} else {
		var err error
		match, err = daemon.Is(target)
		if err != nil {
			return err
		}
	}

	if match {
		log.WithField("event", event).WithField("raw", string(message.Body)).Debug("Handle event")
		data, err := bodyToData(message.Body)
		if err != nil {
			log.WithError(err).Error("Unable to read data")
			return err
		}

		switch event {
		case "delete_requested":
			deleteChan <- sidekick.ReloadEvent{F: f, Message: data}
		case "commit_requested":
			reloadChan <- sidekick.ReloadEvent{F: f, Message: data}
		case "commit_slave_completed":
			reloadChan <- sidekick.ReloadEvent{F: f, Message: data}
		case "commit_completed":
			f(data)
		}

	} else {
		log.WithField("event", event).Debug("Ignore event")
	}

	return nil
}

func onCommitRequested(message *nsq.Message) error {
	return filteredHandler("commit_requested", message, "slave", reloadSlave)
}
func onCommitSlaveRequested(message *nsq.Message) error {
	return filteredHandler("commit_slave_completed", message, "master", reloadMaster)
}
func onCommitCompleted(message *nsq.Message) error {
	return filteredHandler("commit_completed", message, "slave", logAndForget)
}
func onDeleteRequested(message *nsq.Message) error {
	return filteredHandler("delete_requested", message, "any", deleteHaproxy)
}

// logAndForget is a generic function to just log event
func logAndForget(data *sidekick.EventMessageWithConf) error {
	log.WithFields(data.Context().Fields()).Debug("Commit completed")
	return nil
}

func reloadSlave(data *sidekick.EventMessageWithConf) error {
	return reloadHaProxy(data, false)
}

func reloadMaster(data *sidekick.EventMessageWithConf) error {
	return reloadHaProxy(data, true)
}

func deleteHaproxy(data *sidekick.EventMessageWithConf) error {
	context := data.Context()
	hap := sidekick.NewHaproxy("", properties, context)
	err := hap.Stop()
	if err != nil {
		return err
	}
	err = hap.Delete()
	return err
}

// reload an haproxy with content of data in according to role (slave or master)
func reloadHaProxy(data *sidekick.EventMessageWithConf, isMaster bool) error {
	context := data.Context()
	var hap sidekick.Loadbalancer
	var source string
	if (isMaster) {
		hap = haFactory.CreateHaproxy("master", context)
		source = "sidekick-" + properties.ClusterId + "-master"
	} else {
		hap = haFactory.CreateHaproxy("slave", context)
		source = "sidekick-" + properties.ClusterId + "-slave"
	}

	status, err := hap.ApplyConfiguration(data)
	if err == nil {
		if status != sidekick.UNCHANGED {
			elapsed := time.Now().Sub(lastSyslogReload)
			if elapsed.Seconds() > 10 {
				syslog.Restart()
				lastSyslogReload = time.Now()
			} else {
				log.WithField("elapsed time in second", elapsed.Seconds()).Debug("skip syslog reload")
			}
		}
		if (isMaster || hap.Fake()) {
			publishMessage("commit_completed_", data.Clone(source), context)
		} else {
			publishMessage("commit_slave_completed_", data.CloneWithConf(source), context)
		}
	} else {
		log.WithFields(context.Fields()).WithError(err).Error("Commit failed")
		publishMessage("commit_failed_", data.Clone(source), context)
	}
	return nil
}

// Unmarshal json to EventMessage
func bodyToData(jsonStream []byte) (*sidekick.EventMessageWithConf, error) {
	dec := json.NewDecoder(bytes.NewReader(jsonStream))
	var message sidekick.EventMessageWithConf
	err := dec.Decode(&message)
	return &message, err
}

func publishMessage(topic_prefix string, data interface{}, context sidekick.Context) error {
	jsonMsg, _ := json.Marshal(data)
	topic := topic_prefix + properties.ClusterId
	log.WithFields(context.Fields()).WithField("topic", topic).WithField("payload", string(jsonMsg)).Debug("Publish")
	return producer.Publish(topic, []byte(jsonMsg))
}
