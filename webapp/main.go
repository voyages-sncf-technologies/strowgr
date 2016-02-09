package main

import (
	"flag"
	"fmt"
	"log"
	"net/http"
)

var (
	instance = flag.String("instance", "default", "Instance name")
	port = "50080"
)

func main() {

	flag.Parse()

	http.HandleFunc("/", func(writer http.ResponseWriter, request *http.Request) {
		fmt.Fprintf(writer, "%s\n", *instance)
	})
	log.Println("Starting " + *instance + " on port " + port)
	if err := http.ListenAndServe(fmt.Sprintf(":" + port), nil); err != nil {
		log.Fatal("ListenAndServe:", err)
	}

}