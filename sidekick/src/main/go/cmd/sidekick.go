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
	"sort"
)

var (
	configFile = flag.String("config", "sidekick.conf", "Configuration file")
	version = flag.Bool("version", false, "Print current version")
	verbose = flag.Bool("verbose", false, "Log in verbose mode")
	logCompact = flag.Bool("log-compact", false, "compacting log")
	mono = flag.Bool("mono", false, "only one haproxy instance which play slave/master roles.")
	fake = flag.String("fake", "", "Force response without reload for testing purpose. 'yesman': always say ok, 'drunk': random status/errors for entrypoint updates. Just for test purpose.")
	config = nsq.NewConfig()
	properties       *sidekick.Config
	producer         *nsq.Producer
	syslog           *sidekick.Syslog
	reloadChan = make(chan sidekick.ReloadEvent)
	deleteChan = make(chan sidekick.ReloadEvent)
	lastSyslogReload = time.Now()
	haFactory *sidekick.LoadbalancerFactory
)

type SdkLogger struct {
	logrus *log.Logger
}

func (sdkLogger SdkLogger) Output(calldepth int, s string) error {
	log.WithField("type", "nsq driver").Info(s)
	return nil
}

func main() {
	flag.Parse()

	if *logCompact {
		log.SetFormatter(&CompactFormatter{})
	} else {
		log.SetFormatter(&log.TextFormatter{})
	}

	if *version {
		println(sidekick.VERSION)
		os.Exit(0)
	}

	if *verbose {
		log.SetLevel(log.DebugLevel)
		log.WithField("loglevel", "debug").Info("Change loglevel")
	} else {
		log.SetLevel(log.InfoLevel)
		log.WithField("loglevel", "info").Info("Change loglevel")
	}

	loadProperties()

	haFactory = sidekick.NewLoadbalancerFactory()
	haFactory.Fake = *fake
	haFactory.Properties = properties

	syslog = sidekick.NewSyslog(properties)
	syslog.Init()
	log.WithFields(log.Fields{
		"id":     properties.Id,
		"clusterId":     properties.ClusterId,
	}).Info("Starting sidekick")

	producer, _ = nsq.NewProducer(properties.ProducerAddr, config)
	nsqlogger := log.New()
	nsqlogger.Formatter = log.StandardLogger().Formatter
	nsqlogger.Level = log.WarnLevel
	producer.SetLogger(SdkLogger{logrus: nsqlogger}, nsq.LogLevelWarning)

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
		consumer, _ := nsq.NewConsumer(fmt.Sprintf("delete_requested_%s", properties.ClusterId), properties.Id, config)
		consumer.SetLogger(SdkLogger{logrus: nsqlogger}, nsq.LogLevelWarning)
		consumer.AddHandler(nsq.HandlerFunc(onDeleteRequested))
		err := consumer.ConnectToNSQLookupds(properties.LookupdAddresses)
		if err != nil {
			log.Panic("Could not connect")
		}
	}()

	// Start slave consumer
	go func() {
		defer wg.Done()
		wg.Add(1)
		consumer, _ := nsq.NewConsumer(fmt.Sprintf("commit_requested_%s", properties.ClusterId), properties.Id, config)
		consumer.SetLogger(SdkLogger{logrus: nsqlogger}, nsq.LogLevelWarning)
		consumer.AddHandler(nsq.HandlerFunc(onCommitRequested))
		err := consumer.ConnectToNSQLookupds(properties.LookupdAddresses)
		if err != nil {
			log.Panic("Could not connect")
		}
	}()

	// Start master consumer
	go func() {
		defer wg.Done()
		wg.Add(1)
		consumer, _ := nsq.NewConsumer(fmt.Sprintf("commit_slave_completed_%s", properties.ClusterId), properties.Id, config)
		consumer.SetLogger(SdkLogger{logrus: nsqlogger}, nsq.LogLevelWarning)
		consumer.AddHandler(nsq.HandlerFunc(onCommitSlaveRequested))
		err := consumer.ConnectToNSQLookupds(properties.LookupdAddresses)
		if err != nil {
			log.Panic("Could not connect")
		}
	}()

	// Start complete consumer
	go func() {
		defer wg.Done()
		wg.Add(1)
		consumer, _ := nsq.NewConsumer(fmt.Sprintf("commit_completed_%s", properties.ClusterId), properties.Id, config)
		consumer.AddHandler(nsq.HandlerFunc(onCommitCompleted))
		err := consumer.ConnectToNSQLookupds(properties.LookupdAddresses)
		consumer.SetLogger(SdkLogger{logrus: nsqlogger}, nsq.LogLevelWarning)
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

	for _, topic := range topics {
		// create the topic
		log.WithField("topic", topic).WithField("clusterId", properties.ClusterId).Info("Creating topic")
		url := fmt.Sprintf("%s/topic/create?topic=%s_%s", properties.ProducerRestAddr, topic, properties.ClusterId)
		respTopic, err := http.PostForm(url, nil)
		if err == nil && respTopic.StatusCode == 200 {
			log.WithField("topic", topic).WithField("clusterId", properties.ClusterId).Debug("topic created")
		} else {
			log.WithField("topic", topic).WithField("clusterId", properties.ClusterId).WithError(err).Panic("topic can't be created")
		}

		// create the channels of the topics
		log.WithField("channel", properties.Id).Info("Creating channel")
		url = fmt.Sprintf("%s/channel/create?topic=%s_%s&channel=%s", properties.ProducerRestAddr, topic, properties.ClusterId, properties.Id)
		respChannel, err := http.PostForm(url, nil)
		if err == nil && respChannel.StatusCode == 200 {
			log.WithField("channel", properties.Id).WithField("topic", topic).Info("channel created")
		} else {
			// retry all for this topic if channel creation failed
			log.WithField("topic", topic).WithField("channel", properties.Id).WithField("clusterId", properties.ClusterId).WithError(err).Panic("channel can't be created")
		}

	}
}

// loadProperties load properties file
func loadProperties() {
	properties = sidekick.DefaultConfig()
	if _, err := toml.DecodeFile(*configFile, properties); err != nil {
		log.Fatal(err)
		os.Exit(1)
	}
	len := len(properties.HapHome)
	if properties.HapHome[len - 1] == '/' {
		properties.HapHome = properties.HapHome[:len - 1]
	}
}

func filteredHandler(event string, message *nsq.Message, f sidekick.HandlerFunc) error {
	defer message.Finish()

	if *logCompact {
		log.WithField("event", event).WithField("nsq id", message.ID).Debug("new received message from nsq")
	} else {
		log.WithField("event", event).WithField("nsq id", message.ID).WithField("raw", string(message.Body)).Debug("new received message from nsq")
	}
	data, err := bodyToData(message.Body)
	if err != nil {
		log.WithError(err).Error("unable to read body from message")
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

	return nil
}

func onCommitRequested(message *nsq.Message) error {
	return filteredHandler("commit_requested", message, reloadSlave)
}
func onCommitSlaveRequested(message *nsq.Message) error {
	return filteredHandler("commit_slave_completed", message, reloadMaster)
}
func onCommitCompleted(message *nsq.Message) error {
	return filteredHandler("commit_completed", message, logAndForget)
}
func onDeleteRequested(message *nsq.Message) error {
	return filteredHandler("delete_requested", message, deleteHaproxy)
}

// logAndForget is a generic function to just log event
func logAndForget(data *sidekick.EventMessageWithConf) error {
	data.Context().Fields(log.Fields{}).Debug("receive commit completed event")
	return nil
}

func reloadSlave(data *sidekick.EventMessageWithConf) error {
	isMaster, err := properties.IsMaster(data.Conf.Bind)
	if err != nil {
		log.WithField("bind", data.Conf.Bind).WithError(err).Info("can't find if binding to vip")
	}
	if isMaster && !*mono {
		return nil
	} else {
		return reloadHaProxy(data, false)
	}
}

func reloadMaster(data *sidekick.EventMessageWithConf) error {
	isMaster, err := properties.IsMaster(data.Conf.Bind)
	if err != nil {
		log.WithField("bind", data.Conf.Bind).WithError(err).Info("can't find if binding to vip")
	}
	if isMaster {
		return reloadHaProxy(data, true)
	} else {
		return nil
	}
}

func deleteHaproxy(data *sidekick.EventMessageWithConf) error {
	context := data.Context()
	hap := sidekick.NewHaproxy(properties, context)
	err := hap.Stop()
	if err != nil {
		return err
	}
	return hap.Delete()
}

// reload an haproxy with content of data in according to role (slave or master)
func reloadHaProxy(data *sidekick.EventMessageWithConf, masterRole bool) error {
	context := data.Context()
	var hap sidekick.Loadbalancer
	hap = haFactory.CreateHaproxy(context)
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
		if (masterRole || hap.Fake() || *mono) {
			publishMessage("commit_completed_", data.Clone(properties.Id), context)
		} else {
			publishMessage("commit_slave_completed_", data.CloneWithConf(properties.Id), context)
		}
	} else {
		context.Fields(log.Fields{}).WithError(err).Error("Commit failed")
		publishMessage("commit_failed_", data.Clone(properties.Id), context)
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
	if *logCompact {
		context.Fields(log.Fields{"topic": topic}).Debug("publish on topic")
	} else {
		context.Fields(log.Fields{"topic": topic, "payload": string(jsonMsg)}).Debug("publish on topic")
	}
	return producer.Publish(topic, []byte(jsonMsg))
}

// log formatter

type CompactFormatter struct {

}

func (f *CompactFormatter) Format(entry *log.Entry) ([]byte, error) {
	var keys []string = make([]string, 0, len(entry.Data))

	b := &bytes.Buffer{}

	b.WriteString(entry.Time.Format(time.RFC3339))
	b.WriteByte(' ')
	b.WriteByte(entry.Level.String()[0])
	b.WriteByte(' ')

	if cid, ok := entry.Data["correlationId"]; ok {
		fmt.Fprintf(b, "%7s", (cid.(string))[:7])
	} else {
		b.WriteString("       ")
	}
	b.WriteByte(' ')

	if app, ok := entry.Data["application"]; ok {
		fmt.Fprintf(b, "%5s", app)
	} else {
		b.WriteString("     ")
	}
	b.WriteByte(' ')

	if pltf, ok := entry.Data["platform"]; ok {
		fmt.Fprintf(b, "%5s", pltf)
	} else {
		b.WriteString("     ")
	}
	b.WriteByte(' ')

	length := len(entry.Message)
	if length > 40 {
		length = 40
	}
	fmt.Fprintf(b, "'%-40s' ", entry.Message[:length])

	for k := range entry.Data {
		if k == "application" || k == "correlationId" || k == "platform" || k == "timestamp" {
			continue
		} else {
			keys = append(keys, k)
		}
	}
	sort.Strings(keys)
	for _, k := range keys {
		switch value := entry.Data[k].(type) {
		case string:
			fmt.Fprintf(b, "'%s=%s' ", k, entry.Data[k].(string))
		case error:
			fmt.Fprintf(b, "%q ", value)
		default:
			fmt.Fprintf(b, "'%s=%s' ", k, value)
		}

	}

	b.WriteByte('\n')
	return b.Bytes(), nil
}
