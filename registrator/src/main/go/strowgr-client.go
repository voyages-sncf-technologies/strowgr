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
package registrator

import (
	"bytes"
	"encoding/json"
	"fmt"
	log "github.com/Sirupsen/logrus"
	"io/ioutil"
	"net/http"
)

type Instance struct {
	Id       string            `json:"id"`
	Hostname string            `json:"hostname"`
	Ip       string            `json:"ip"`
	Port     string            `json:"port"`
	App      string            `json:"-"`
	Platform string            `json:"-"`
	Service  string            `json:"-"`
	Context  map[string]string `json:"context"`
}

func NewInstance() *Instance {
	return &Instance{
		Context: make(map[string]string),
	}
}

func (instance *Instance) Register(adminUrl string) {
	log.WithFields(log.Fields{
		"id":          instance.Id,
		"application": instance.App,
		"platform":    instance.Platform,
		"service":     instance.Service,
	}).Info("Register")

	var url = fmt.Sprintf("%s/api/entrypoints/%s/%s/backend/%s/register-server", adminUrl, instance.App, instance.Platform, instance.Service)
	json, _ := json.Marshal(instance)
	req, err := http.NewRequest("POST", url, bytes.NewBuffer(json))
	req.Header.Set("Content-Type", "application/json")

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		log.WithError(err).WithField("url", url).WithField("json", string(json)).Error("Error requesting")
		return
	} else {
		log.WithField("url", url).WithField("body", string(json)).Debug("http post HaaS admin")
	}
	defer resp.Body.Close()

	ioutil.ReadAll(resp.Body)
}
