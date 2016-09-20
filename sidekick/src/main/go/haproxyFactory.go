package sidekick

import log "github.com/Sirupsen/logrus"

type Loadbalancer interface {
	ApplyConfiguration(data *EventMessageWithConf) (int, error)
	Stop() error
	Delete() error
	Fake() bool
}

type LoadbalancerFactory struct {
	Fake       string
	Properties *Config
}

func NewLoadbalancerFactory() *LoadbalancerFactory {
	return &LoadbalancerFactory{
		Fake: "none",
	}
}

func (factory *LoadbalancerFactory) CreateHaproxy(role string, context Context) Loadbalancer {
	if factory.Fake == "drunk" {
		log.Info("mode drunk")
		return &DrunkHaproxy{
			role:    role,
			context: context,
		}
	} else if ( factory.Fake == "yesman") {
		log.Info("mode yesman")
		return &YesmanHaproxy{}
	} else {
		log.Info("mode normal")
		return NewHaproxy(role, factory.Properties, context)
	}
}
