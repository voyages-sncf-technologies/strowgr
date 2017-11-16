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
package com.vsct.dt.strowgr.admin.gui.mapping.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.strowgr.admin.gui.security.model.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

/**
 * Json representation of {@code EntryPointConfiguration}.
 * <p>
 * Created by william_montaz on 09/02/2016.
 */
public class EntryPointWithPortsMappingJson extends EntryPointMappingJson {

    private final Integer syslogPort;

    @JsonCreator
    public EntryPointWithPortsMappingJson(@JsonProperty("user") User user,
    									  @JsonProperty("haproxy") String haproxy,
                                          @JsonProperty("hapUser") String hapUser,
                                          @JsonProperty("haproxyVersion") String haproxyVersion,
                                          @JsonProperty("bindingId") int bindingId,
                                          @JsonProperty("syslogPort") Integer syslogPort,
                                          @JsonProperty("frontends") Set<EntryPointFrontendWithPortMappingJson> frontends,
                                          @JsonProperty("backends") Set<EntryPointBackendMappingJson> backends,
                                          @JsonProperty("context") Map<String, String> context) {
        super(
                haproxy,
                hapUser,
                haproxyVersion,
                bindingId,
                frontends.stream().map(identity()).collect(Collectors.toSet()),
                backends.stream().map(identity()).collect(Collectors.toSet()),
                context
        );
        this.syslogPort = syslogPort;
    }

    @JsonCreator
    public EntryPointWithPortsMappingJson(@JsonProperty("haproxy") String haproxy,
                                          @JsonProperty("hapUser") String hapUser,
                                          @JsonProperty("haproxyVersion") String haproxyVersion,
                                          @JsonProperty("bindingId") int bindingId,
                                          @JsonProperty("syslogPort") Integer syslogPort,
                                          @JsonProperty("frontends") Set<EntryPointFrontendWithPortMappingJson> frontends,
                                          @JsonProperty("backends") Set<EntryPointBackendMappingJson> backends,
                                          @JsonProperty("context") Map<String, String> context) {
        super(
                haproxy,
                hapUser,
                haproxyVersion,
                bindingId,
                frontends.stream().map(identity()).collect(Collectors.toSet()),
                backends.stream().map(identity()).collect(Collectors.toSet()),
                context
        );
        this.syslogPort = syslogPort;
    }
    
    
    public Map<String, Integer> generatePortMapping() {
        HashMap<String, Integer> mapping = new HashMap<>();
        if (this.syslogPort != null) mapping.put(syslogPortId(), this.syslogPort);
        this.getFrontends().forEach(f -> {
            if (((EntryPointFrontendWithPortMappingJson) f).getPort() != null) {
                mapping.put(f.getId(),
                        ((EntryPointFrontendWithPortMappingJson) f).getPort()
                );
            }
        });
        return mapping;
    }

}
