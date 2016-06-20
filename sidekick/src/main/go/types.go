/*
 *  Copyright (C) 2016 VSCT
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
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
	HapVersion       string
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
	Conf struct {
		Haproxy []byte `json:"haproxy"`
		Syslog  []byte `json:"syslog"`
	} `json:"conf"`
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
