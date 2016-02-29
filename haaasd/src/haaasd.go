package main

import (
	"./haaasd"
	"bytes"
	"encoding/json"
	"flag"
	"fmt"
	"github.com/BurntSushi/toml"
	"github.com/bitly/go-nsq"
	"log"
	"os"
	"os/signal"
	"sync"
	"syscall"
	"net/http"
	"time"
)

var (
	ip = flag.String("ip", "4.3.2.1", "Node ip address")
	configFile = flag.String("config", "haaas.conf", "Configuration file")
	versionFlag = flag.Bool("version", false, "Print current version")
	config = nsq.NewConfig()
	properties haaasd.Config
	daemon      *haaasd.Daemon
	producer    *nsq.Producer
)

func main() {

	flag.Parse()

	if *versionFlag {
		println(haaasd.AppVersion)
		os.Exit(0)
	}

	loadProperties()

	daemon = haaasd.NewDaemon(&properties)

	log.Printf("Starting haaasd (%s) with id %v", properties.Status, properties.NodeId())

	producer, _ = nsq.NewProducer(properties.ProducerAddr, config)

	initProducer()
	time.Sleep(1000 * time.Millisecond)

	var wg sync.WaitGroup
	// Start http API
	restApi := haaasd.NewRestApi(&properties)
	go func() {
		defer wg.Done()
		wg.Add(1)
		err := restApi.Start()
		if err != nil {
			log.Fatal("Cannot start api")
		}
	}()

	//	 Start slave consumer
	go func() {
		defer wg.Done()
		wg.Add(1)
		consumer, _ := nsq.NewConsumer(fmt.Sprintf("commit_requested_%s", properties.ClusterId), properties.NodeId(), config)
		consumer.AddHandler(nsq.HandlerFunc(onCommitRequested))
		err := consumer.ConnectToNSQLookupd(properties.LookupdAddr)
		if err != nil {
			log.Panic("Could not connect")
		}
	}()

	//	 Start master consumer
	go func() {
		defer wg.Done()
		wg.Add(1)
		consumer, _ := nsq.NewConsumer(fmt.Sprintf("commit_slave_completed_%s", properties.ClusterId), properties.NodeId(), config)
		consumer.AddHandler(nsq.HandlerFunc(onCommitSlaveRequested))
		err := consumer.ConnectToNSQLookupd(properties.LookupdAddr)
		if err != nil {
			log.Panic("Could not connect")
		}
	}()

	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, os.Interrupt, syscall.SIGTERM, syscall.SIGQUIT)
	select {
	case signal := <-sigChan:
		log.Printf("Got signal: %v\n", signal)
	}
	restApi.Stop()

	log.Printf("Waiting on server to stop\n")
	wg.Wait()
}

func initProducer() {
	// Create required topics
	topics := []string{"commit_requested", "commit_slave_completed", "commit_completed", "commit_failed"}

	topicChan := make(chan string, len(topics))
	for i := range topics {
		topicChan <- topics[i]
	}
	left := len(topics)
	for left > 0 {
		topic := <-topicChan
		log.Printf("Creating %s", topic)
		resp, err := http.PostForm(properties.ProducerRestAddr + "/topic/create?topic=" + topic + "_" + properties.ClusterId, nil)
		if err != nil || resp.StatusCode != 200 {
			topicChan <- topic
		}else {
			left--
			log.Printf("Topic %s created", topic)
		}
	}
}

// Load properties from file
func loadProperties() {
	if _, err := toml.DecodeFile(*configFile, &properties); err != nil {
		log.Fatal(err)
		os.Exit(1)
	}
	properties.IpAddr = *ip
	len := len(properties.HapHome)
	if properties.HapHome[len - 1] == '/' {
		properties.HapHome = properties.HapHome[:len - 1]
	}
}

func filteredHandler(event string, message *nsq.Message, target string, f haaasd.HandlerFunc) error {
	defer message.Finish()
	match, err := daemon.Is(target)
	if err != nil {
		return err
	}

	if match {
		log.Printf("Handle %s event:\n%s", event, message.Body)
		data, err := bodyToData(message.Body)
		if err != nil {
			log.Print("Unable to read data\n%s", err)
			return err
		}
		f(data)
	} else {
		log.Printf("Ignore event %s", event)
	}

	return nil
}

func onCommitRequested(message *nsq.Message) error {
	return filteredHandler("commit_requested", message, "slave", reloadSlave)
}
func onCommitSlaveRequested(message *nsq.Message) error {
	return filteredHandler("commit_slave_completed_", message, "master", reloadMaster)
}

func reloadSlave(data *haaasd.EventMessage) error {
	hap := haaasd.NewHaproxy(&properties, data.Application, data.Platform, data.HapVersion)
	err := hap.ApplyConfiguration(data)
	if err == nil {
		publishMessage("commit_slave_completed_", data)
	} else {
		log.Print(err)
		publishMessage("commit_failed_", map[string]string{"application": data.Application, "platform": data.Platform, "correlationid": data.Correlationid})
	}
	return nil
}

func reloadMaster(data *haaasd.EventMessage) error {
	hap := haaasd.NewHaproxy(&properties, data.Application, data.Platform, data.HapVersion)
	err := hap.ApplyConfiguration(data)
	if err == nil {
		publishMessage("commit_completed_", map[string]string{"application": data.Application, "platform": data.Platform, "correlationid": data.Correlationid})
	} else {
		log.Print(err)
		publishMessage("commit_failed_", map[string]string{"application": data.Application, "platform": data.Platform, "correlationid": data.Correlationid})
	}
	return nil
}

// Unmarshal json to EventMessage
func bodyToData(jsonStream []byte) (*haaasd.EventMessage, error) {
	dec := json.NewDecoder(bytes.NewReader(jsonStream))
	var message haaasd.EventMessage
	err := dec.Decode(&message)
	return &message, err
}

func publishMessage(topic_prefix string, data interface{}) error {
	jsonMsg, _ := json.Marshal(data)
	topic := topic_prefix + properties.ClusterId
	log.Printf("Publish to %s : %s ", topic, jsonMsg)
	return producer.Publish(topic, []byte(jsonMsg))
}
