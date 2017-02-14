/*
 * Copyright (C) 2016 VSCT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vsct.dt.strowgr.admin.repository.consul.mapping.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPointBackendServer;

import java.util.Map;

/**
 * Json mapping of {@code EntryPointBackendServer}.
 *
 * Created by william_montaz on 09/02/2016.
 */
public class EntryPointBackendServerMappingJson extends EntryPointBackendServer {

    @JsonCreator
    public EntryPointBackendServerMappingJson(@JsonProperty("id") String id,
                                              @JsonProperty("hostname") String hostname,
                                              @JsonProperty("ip") String ip,
                                              @JsonProperty("port") String port,
                                              @JsonProperty("context") Map<String, String> context,
                                              @JsonProperty("contextOverride") Map<String, String> contextOverride) {
        super(id, ip, port, context, contextOverride);
    }

    public EntryPointBackendServerMappingJson(EntryPointBackendServer entryPointBackendServer){
        this(
                entryPointBackendServer.getId(),
                entryPointBackendServer.getHostname(),
                entryPointBackendServer.getIp(),
                entryPointBackendServer.getPort(),
                entryPointBackendServer.getContext(),
                entryPointBackendServer.getContextOverride()
        );
    }

}
