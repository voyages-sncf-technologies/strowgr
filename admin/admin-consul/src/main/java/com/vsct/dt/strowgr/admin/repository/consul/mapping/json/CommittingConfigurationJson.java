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

package com.vsct.dt.strowgr.admin.repository.consul.mapping.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

public class CommittingConfigurationJson extends EntryPoint {

    private final String correlationId;

    @JsonCreator
    public CommittingConfigurationJson(@JsonProperty("correlationId") String correlationId,
                                       @JsonProperty("haproxy") String haproxy,
                                       @JsonProperty("hapUser") String hapUser,
                                       @JsonProperty("frontends") Set<EntryPointFrontendMappingJson> frontends,
                                       @JsonProperty("backends") Set<EntryPointBackendMappingJson> backends,
                                       @JsonProperty("context") Map<String, String> context) {
        super(haproxy,
                hapUser,
                frontends.stream().map(identity()).collect(Collectors.toSet()),
                backends.stream().map(identity()).collect(Collectors.toSet()),
                context);
        this.correlationId = correlationId;
    }

    public CommittingConfigurationJson(String correlationId, EntryPoint configuration) {
        super(configuration.getHaproxy(),
                configuration.getHapUser(),
                configuration.getFrontends(),
                configuration.getBackends(),
                configuration.getContext());
        this.correlationId = correlationId;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
