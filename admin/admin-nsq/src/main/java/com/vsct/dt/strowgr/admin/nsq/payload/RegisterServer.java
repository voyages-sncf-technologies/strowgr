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
import com.vsct.dt.strowgr.admin.nsq.payload.fragment.Header;
import com.vsct.dt.strowgr.admin.nsq.payload.fragment.Server;

public class RegisterServer {

    @JsonProperty("header")
    private Header header;

    @JsonProperty("server")
    Server server;

    @JsonCreator
    public RegisterServer(String correlationId, String application, String platform) {
        header = new Header(correlationId, application, platform);
    }

    @JsonCreator
    public RegisterServer() {
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public Server getServer() {
        return server;
    }

    public Header getHeader() {
        return header;
    }
}
