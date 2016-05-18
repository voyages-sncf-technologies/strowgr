package sidekick

import (
	"fmt"
	log "github.com/Sirupsen/logrus"
	"net"
	"net/http"
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

func (api *RestApi) Start() error {
	sm := http.NewServeMux()
	sm.HandleFunc("/uuid", func(writer http.ResponseWriter, request *http.Request) {
		log.Debug("GET /uuid")
		fmt.Fprintf(writer, "%s\n", api.properties.IpAddr)
	})

	listener, err := net.Listen("tcp4", fmt.Sprintf(":%d", api.properties.Port))
	if err != nil {
		log.WithError(err).Fatal(err)
	}
	api.listener, err = NewListener(listener)
	if err != nil {
		return err
	}

	log.WithField("port", api.properties.Port).Info("Start listening")
	http.Serve(api.listener, sm)

	return nil
}

func (api *RestApi) Stop() {
	if api.listener != nil {
		api.listener.Stop()
	}
}
