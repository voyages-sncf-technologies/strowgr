package main

import (
	"flag"
	"fmt"
	"github.com/bitly/go-nsq"
	"io/ioutil"
	"log"
	"net/http"
	"encoding/json"
	"os"
	"bytes"
	"encoding/base64"
)

var (
	clusterId = flag.String("cluster-id", "default-name", "Cluster id to handle")
	vip = flag.String("vip", "127.0.0.1", "Cluster vip address")
	ip = flag.String("ip", "4.3.2.1", "Node ip address")
	port = flag.String("port", "58080", "Listen port")
	lookupd = flag.String("lookupd", "floradora:50161", "NSQ Lookup daemon")
	haproxyConf string
	nodeId string
	config = nsq.NewConfig()
)

type EventMessage struct {
	Correlationid string
	Conf          []byte
	Timestamp     int64
}


func main() {

	flag.Parse()

	nodeId = fmt.Sprintf("%s-%s", *clusterId, *ip)

	haproxyConf = "haproxy-" + *clusterId + ".conf";
	log.Printf("Starting haaasd for cluster %v", nodeId)

	startTryUpdateConsumer()

	starRestAPI()
}

func check(err error) {
	if err != nil {
		log.Fatal(err)
	}
}

func startTryUpdateConsumer() {
	tryUpdateConsumer, _ := nsq.NewConsumer(fmt.Sprintf("try_update_%s", *clusterId), nodeId, config)

	tryUpdateConsumer.AddHandler(nsq.HandlerFunc(func(message *nsq.Message) error {
		isSlave, err := isSlave()
		if err != nil {
			message.Finish()
			return err
		}

		if isSlave {
			log.Print("I am a slave")
			log.Printf("%s", message.Body)
			data, err := bodyToDatas(message.Body)
			check(err)

			applyConfiguration(data.Conf)
		}

		return nil
	}))

	err := tryUpdateConsumer.ConnectToNSQLookupd(*lookupd)
	if err != nil {
		log.Panic("Could not connect")
	}
}

func applyConfiguration(newConf []byte) {
	// /appl/hapadm/DTC/version-1/
	os.Rename(haproxyConf, "version-1/" + haproxyConf)
	err := ioutil.WriteFile(haproxyConf, newConf, 0644)
	if err != nil {
		log.Fatal(err); return
	}
}

func bodyToDatas(jsonStream []byte) (EventMessage, error) {
	log.Print("json: %s", jsonStream)

	dec := json.NewDecoder(bytes.NewReader(jsonStream))
	var message EventMessage
	dec.Decode(&message)
	log.Print("%+v", message.Conf)

	decConf, err := base64.StdEncoding.DecodeString(string(message.Conf))
	check(err)
	log.Print("%s", decConf)
	log.Print("%+v", message)
	//	message.Conf = conf
	return message, nil
}

func starRestAPI() {
	http.HandleFunc("/real-ip", func(writer http.ResponseWriter, request *http.Request) {
		log.Printf("GET /real-ip")
		fmt.Fprintf(writer, "%s\n", *ip)
	})

	if err := http.ListenAndServe(fmt.Sprintf(":%s", *port), nil); err != nil {
		log.Fatal("ListenAndServe:", err)
	}
}

func isMaster() (bool, error) {
	resp, err := http.Get(fmt.Sprintf("http://%s:%s/real-ip", *vip, *port))
	if err != nil {
		log.Fatal(err)
	}
	defer resp.Body.Close()
	body, err := ioutil.ReadAll(resp.Body)
	log.Printf("master ip: %s", body)
	return string(body) == *ip, nil
}

func isSlave() (bool, error) {
	isMaster, err := isMaster()
	return ! isMaster, err
}
