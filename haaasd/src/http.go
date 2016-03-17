package haaasd
import (
	"net/http"
	"log"
	"fmt"
	"net"
)

type RestApi struct {
	properties *Config
	listener   *StoppableListener
}

func NewRestApi(properties *Config) *RestApi {
	api := &RestApi{
		properties: properties,
	}
	return api
}

func (api *RestApi)Start() (error) {
	sm := http.NewServeMux()
	sm.HandleFunc("/uuid", func(writer http.ResponseWriter, request *http.Request) {
		log.Printf("GET /uuid")
		fmt.Fprintf(writer, "%s\n", api.properties.IpAddr)
	})

	listener, err := net.Listen("tcp4", fmt.Sprintf(":%d", api.properties.Port))
	if err != nil {
		log.Fatal(err)
	}
	api.listener, err = NewListener(listener)
	if err != nil {
		return err
	}

	log.Printf("Start listening on port %d", api.properties.Port)
	http.Serve(api.listener, sm)

	return nil
}

func (api *RestApi)Stop() {
	if (api.listener != nil) {
		api.listener.Stop()
	}
}
