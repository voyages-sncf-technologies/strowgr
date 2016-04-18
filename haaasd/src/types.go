package haaasd

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

func DefaultConfig() (*Config) {
	return &Config{
		Port:5000,
		HapHome:"/HOME/hapadm",
		ClusterId: "default-name",
	}
}

func (config *Config) NodeId() string {
	return config.ClusterId + "-" + config.Status
}

type EventMessage struct {
	Correlationid  string	`json:"correlationId"`
	Conf           []byte	`json:"conf"`
	Timestamp      int64	`json:"timestamp"`
	Application    string	`json:"application"`
	Platform       string	`json:"platform"`
	HapVersion     string	`json:"hapVersion"`
	SyslogFragment []byte	`json:"syslogConf"`
}

const (
	STATE_IDLE = iota
	STATE_RELOADING_SLAVE = iota
	STATE_ROLLBACK_SLAVE = iota
	STATE_RELOADING_MASTER = iota
	STATE_ROLLBACK_MASTER = iota
	STATE_ERROR = iota
)

type EventHandler interface {
	HandleMessage(data *EventMessage) error
}
type HandlerFunc func(data *EventMessage) error

func (h HandlerFunc) HandleMessage(m *EventMessage) error {
	return h(m)
}
