package sidekick

type Loadbalancer interface {
	ApplyConfiguration(data *EventMessageWithConf) (int, error)
	Stop() error
	Delete() error
}

type LoadbalancerFactory struct {
	Drunk      bool
	Properties *Config
}

func NewLoadbalancerFactory() *LoadbalancerFactory {
	return &LoadbalancerFactory{
		Drunk: false,
	}
}

func (factory *LoadbalancerFactory) CreateHaproxy(role string, context Context) Loadbalancer {
	if factory.Drunk {
		return &DrunkHaproxy{
			role:    role,
			context: context,
		}
	} else {
		return NewHaproxy(role, factory.Properties, context)
	}
}
