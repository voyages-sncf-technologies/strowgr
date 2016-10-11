package sidekick

import (
	"errors"
	"math/rand"
	log "github.com/Sirupsen/logrus"
)

type DrunkHaproxy struct {
	role    string
	context Context
}

func (hap DrunkHaproxy) ApplyConfiguration(data *EventMessageWithConf) (int, error) {
	var err error
	status := rand.Intn(MAX_STATUS)
	hap.context.Fields(log.Fields{"status": status}).Info("choose a random status")
	if status <= UNCHANGED {
		err = errors.New("blop, a new error...")
	}
	return status, err
}

func (hap DrunkHaproxy) Stop() error {
	hap.context.Fields(log.Fields{}).Info("Stop drunk instance")
	return nil
}
func (hap DrunkHaproxy) Delete() error {
	hap.context.Fields(log.Fields{}).Info("Delete drunk instance")
	return nil
}

func (hap DrunkHaproxy) Fake() bool {
	return true
}