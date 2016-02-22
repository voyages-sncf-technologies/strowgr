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
)

var (
	ip          = flag.String("ip", "4.3.2.1", "Node ip address")
	configFile  = flag.String("config", "haaas.conf", "Configuration file")
	versionFlag = flag.Bool("version", false, "Print current version")
	config      = nsq.NewConfig()
	properties  haaasd.Config
	daemon      *haaasd.Daemon
	producer    *nsq.Producer
)

func main() {

	flag.Parse()

	if *versionFlag {
		println(haaasd.AppVersion)
		os.Exit(0)
	}

	if _, err := toml.DecodeFile(*configFile, &properties); err != nil {
		log.Fatal(err)
		os.Exit(1)
	}
	properties.IpAddr = *ip
	len := len(properties.HapHome)
	if properties.HapHome[len-1] == '/' {
		properties.HapHome = properties.HapHome[:len-1]
	}

	daemon = haaasd.NewDaemon(&properties)

	log.Printf("Starting haaasd (%s) with id %v", properties.Status, properties.NodeId())

	producer, _ = nsq.NewProducer(properties.ProducerAddr, config)

	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, os.Interrupt, syscall.SIGTERM, syscall.SIGQUIT)

	var wg sync.WaitGroup
	restApi, err := haaasd.NewRestApi(&properties)
	if err != nil {
		log.Fatal("Cannot start api")
	}
	// Start API
	go func() {
		defer wg.Done()
		wg.Add(1)
		restApi.Start()
	}()

	//	 Start slave consumer
	go func() {
		defer wg.Done()
		wg.Add(1)
		startTryUpdateConsumer()
	}()

	//	 Start master consumer
	go func() {
		defer wg.Done()
		wg.Add(1)
		startUpdateConsumer()
	}()

	select {
	case signal := <-sigChan:
		fmt.Printf("Got signal: %v\n", signal)
	}
	restApi.Stop()

	fmt.Printf("Waiting on server\n")
	wg.Wait()
}

func startTryUpdateConsumer() {
	tryUpdateConsumer, _ := nsq.NewConsumer(fmt.Sprintf("commit_requested_%s", properties.ClusterId), properties.NodeId(), config)

	tryUpdateConsumer.AddHandler(nsq.HandlerFunc(func(message *nsq.Message) error {
		defer message.Finish()

		isSlave, err := daemon.IsSlave()
		if err != nil {
			return err
		}

		if isSlave {
			log.Printf("Receive commit_requested event %s", message.Body)
			data, err := bodyToData(message.Body)
			if err != nil {
				log.Print("Unable to read data\n%s", string(message.Body))
				return err
			}

			log.Printf("Receive commit_requested event %-v", data)
			hap := haaasd.NewHaproxy(&properties, data.Application, data.Platform)
			err = hap.ApplyConfiguration(data)
			if err == nil {
				publishMessage("commit_slave_completed_", data)
			} else {
				log.Print(err)
				publishMessage("commit_failed_", map[string]string{"application": data.Application, "platform": data.Platform, "correlationid": data.Correlationid})
			}
		}

		return nil
	}))

	err := tryUpdateConsumer.ConnectToNSQLookupd(properties.LookupdAddr)
	if err != nil {
		log.Panic("Could not connect")
	}
}

func startUpdateConsumer() {
	updateConsumer, _ := nsq.NewConsumer("commit_slave_completed_"+properties.ClusterId, properties.NodeId(), config)

	updateConsumer.AddHandler(nsq.HandlerFunc(func(message *nsq.Message) error {
		defer message.Finish()
		isMaster, err := daemon.IsMaster()
		if err != nil {
			return err
		}

		if isMaster {
			log.Printf("Receive commit_slave_completed event %s", message.Body)
			data, err := bodyToData(message.Body)
			if err != nil {
				log.Print("Unable to read data\n%s", string(message.Body))
				return err
			}

			hap := haaasd.NewHaproxy(&properties, data.Application, data.Platform)
			err = hap.ApplyConfiguration(data)

			if err == nil {
				publishMessage("commit_completed_", map[string]string{"application": data.Application, "platform": data.Platform, "correlationid": data.Correlationid})
			} else {
				publishMessage("commit_failed_", map[string]string{"application": data.Application, "platform": data.Platform, "correlationid": data.Correlationid})
				log.Fatal(err)
			}
		}

		return nil
	}))

	err := updateConsumer.ConnectToNSQLookupd(properties.LookupdAddr)
	if err != nil {
		log.Panic("Could not connect")
	}
}

// Unmarshal json to EventMessage
func bodyToData(jsonStream []byte) (haaasd.EventMessage, error) {
	dec := json.NewDecoder(bytes.NewReader(jsonStream))
	var message haaasd.EventMessage
	err := dec.Decode(&message)
	return message, err
}

func publishMessage(topic_prefix string, data interface{}) error {
	jsonMsg, _ := json.Marshal(data)
	topic := topic_prefix + properties.ClusterId
	log.Printf("Publish to %s : %s", topic, jsonMsg)
	return producer.Publish(topic, []byte(jsonMsg))
}
