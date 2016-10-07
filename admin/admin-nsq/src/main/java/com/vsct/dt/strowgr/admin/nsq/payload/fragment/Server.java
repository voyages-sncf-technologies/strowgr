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

package com.vsct.dt.strowgr.admin.nsq.payload.fragment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class Server {

    private final String              id;
    private final String              backendId;
    private final String              ip;
    private final String              port;
    private final Map<String, String> context;

    @JsonCreator
    public Server(@JsonProperty("id") String id,
                  @JsonProperty("backendId") String backendId,
                  @JsonProperty("ip") String ip,
                  @JsonProperty("port") String port,
                  @JsonProperty("context") Map<String, String> context) {
        this.id = id;
        this.backendId = backendId;
        this.ip = ip;
        this.port = port;
        this.context = context;
    }

    public String getId() {
        return id;
    }

    public String getBackendId() {
        return backendId;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public Map<String, String> getContext() {
        return context;
    }
}
