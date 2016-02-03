package haaas

type Config struct {
	LookupdAddr  string
	ProducerAddr string
	ClusterId    string
	Vip          string
	Port         int32
	HapHome      string
	IpAddr       string
	Status       string
}

func (config *Config) NodeId() string {
	return config.ClusterId + "-" + config.IpAddr
}

type EventMessage struct {
	Correlationid string
	Conf          []byte
	Timestamp     int64
	Application   string
	Platform      string
}
