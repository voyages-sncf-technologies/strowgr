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
//	"encoding/base64"
)

var (
	clusterId = flag.String("cluster-id", "default-name", "Cluster id to handle")
	vip = flag.String("vip", "127.0.0.1", "Cluster vip address")
	ip = flag.String("ip", "4.3.2.1", "Node ip address")
	port = flag.String("port", "58080", "Listen port")
	lookupd = flag.String("lookupd", "floradora:50161", "NSQ Lookup daemon")
	pid = flag.String("haproxy-pid", "", "NSQ Lookup daemon")
	hapHome = flag.String("hap-home", "/HOME/appl/hapadm/", "hap home directory")

	haproxyConf string
	nodeId string
	config = nsq.NewConfig()
)

type EventMessage struct {
	Correlationid string
	Conf          []byte
	Timestamp     int64
	Application   string
	Platform      string
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

func getArchiveDirectory() string {
	dir := "version-1"
	os.MkdirAll(dir, 0755)
	return dir
}

func applyConfiguration(newConf []byte) {
	var archives string = getArchiveDirectory()
	// /appl/hapadm/DTC/version-1/
	os.Rename(haproxyConf, archives + "/" + haproxyConf)
	err := ioutil.WriteFile(haproxyConf, newConf, 0644)
	check(err)
}

func bodyToDatas(jsonStream []byte) (EventMessage, error) {
	dec := json.NewDecoder(bytes.NewReader(jsonStream))
	var message EventMessage
	dec.Decode(&message)
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
