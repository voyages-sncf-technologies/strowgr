package sidekick

import (
	log "github.com/Sirupsen/logrus"
)

type YesmanHaproxy struct {
	role    string
	context Context
	fake    bool
}

func (hap YesmanHaproxy) ApplyConfiguration(data *EventMessageWithConf) (int, error) {
	return SUCCESS, nil
}

func (hap YesmanHaproxy) Stop() error {
	log.WithFields(hap.context.Fields()).Info("Stop yesman instance")
	return nil
}
func (hap YesmanHaproxy) Delete() error {
	log.WithFields(hap.context.Fields()).Info("Delete yesman instance")
	return nil
}

func (hap YesmanHaproxy) Fake() bool {
	return true
}

