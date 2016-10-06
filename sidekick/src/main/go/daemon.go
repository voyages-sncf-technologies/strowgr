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
	"net/http"
	"fmt"
	"log"
	"io/ioutil"
)

type Daemon struct {
	Properties *Config
}

func (daemon *Daemon) IsMaster(vip string) (bool, error) {
	resp, err := http.Get(fmt.Sprintf("http://%s:%d/real-ip", vip, daemon.Properties.Port))
	if err != nil {
		log.Fatal(err)
	}
	defer resp.Body.Close()
	body, err := ioutil.ReadAll(resp.Body)
	log.Printf("master id: %s", body)
	return string(body) == daemon.Properties.Id, nil
}

func NewDaemon(properties *Config) *Daemon {
	return &Daemon{
		Properties: properties,
	}
}
