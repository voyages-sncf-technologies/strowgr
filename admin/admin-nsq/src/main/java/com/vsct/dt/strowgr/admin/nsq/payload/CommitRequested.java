/*
 * Copyright (C) 2016 VSCT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.vsct.dt.strowgr.admin.nsq.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.strowgr.admin.nsq.payload.fragment.Conf;

public class CommitRequested extends AbstractPayload{
    @JsonProperty("conf")
    private Conf conf;

    @JsonCreator
    public CommitRequested(String correlationId, String application, String platform, String confBase64, String syslogConfBase64) {
        super(correlationId,application,platform);
        conf = new Conf(confBase64,syslogConfBase64);
    }

    public Conf getConf() {
        return conf;
    }

    public void setConf(Conf conf) {
        this.conf = conf;
    }
}