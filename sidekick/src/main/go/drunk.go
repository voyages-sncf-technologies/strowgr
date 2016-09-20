package sidekick

import (
	"errors"
	log "github.com/Sirupsen/logrus"
	"math/rand"
)

type DrunkHaproxy struct {
	role    string
	context Context
}

func (hap DrunkHaproxy) ApplyConfiguration(data *EventMessageWithConf) (int, error) {
	var err error
	status := rand.Intn(MAX_STATUS)
	log.WithFields(hap.context.Fields()).WithField("status", status).Info("choose a random status")
	if status <= UNCHANGED {
		err = errors.New("blop, a new error...")
	}
	return status, err
}

func (hap DrunkHaproxy) Stop() error {
	log.WithFields(hap.context.Fields()).Info("Stop drunk instance")
	return nil
}
func (hap DrunkHaproxy) Delete() error {
	log.WithFields(hap.context.Fields()).Info("Delete drunk instance")
	return nil
}

func (hap DrunkHaproxy) Fake() bool {
	return true
}