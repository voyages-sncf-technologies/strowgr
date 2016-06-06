package sidekick

import (
	log "github.com/Sirupsen/logrus"
	"time"
)

type Config struct {
	LookupdAddr      string
	ProducerAddr     string
	ProducerRestAddr string
	ClusterId        string
	Vip              string
	Port             int32
	HapHome          string
	IpAddr           string
	Status           string
}

func DefaultConfig() *Config {
	return &Config{
		Port:      5000,
		HapHome:   "/HOME/hapadm",
		ClusterId: "default-name",
	}
}

func (config *Config) NodeId() string {
	return config.ClusterId + "-" + config.Status
}

type EventMessage struct {
	Header struct {
		       CorrelationId string `json:"correlationId"`
		       Application   string `json:"application"`
		       Platform      string `json:"platform"`
		       Timestamp     int64  `json:"timestamp"`
		       Source        string `json:"source"`
	       } `json:"header"`
	Conf   struct {
		       Haproxy    []byte `json:"haproxy"`
		       Syslog     []byte `json:"syslog"`
		       HapVersion string `json:"hapVersion"`
	       }`json:"conf"`
}

// retrieve Context from an EventMessage
func (em EventMessage) Context() Context {
	return Context{
		CorrelationId: em.Header.CorrelationId,
		Timestamp:     em.Header.Timestamp,
		Application:   em.Header.Application,
		Platform:      em.Header.Platform,
	}
}

// context for tracing current process
type Context struct {
	CorrelationId string `json:"correlationId"`
	Timestamp     int64  `json:"timestamp"`
	Application   string `json:"application"`
	Platform      string `json:"platform"`
}

// update the timestamp of the current context
func (ctx Context) UpdateTimestamp() Context {
	ctx.Timestamp = time.Now().UnixNano() / int64(time.Millisecond)
	return ctx
}

// translate Context to Fields for logging purpose
func (ctx Context) Fields() log.Fields {
	return log.Fields{
		"correlationId": ctx.CorrelationId,
		"timestamp":     ctx.UpdateTimestamp().Timestamp,
		"application":   ctx.Application,
		"platform":      ctx.Platform,
	}
}

type ReloadEvent struct {
	Message *EventMessage
	F       func(data *EventMessage) error
}

func (re *ReloadEvent) Execute() error {
	return re.F(re.Message)
}

type EventHandler interface {
	HandleMessage(data *EventMessage) error
}
type HandlerFunc func(data *EventMessage) error

func (h HandlerFunc) HandleMessage(m *EventMessage) error {
	return h(m)
}
