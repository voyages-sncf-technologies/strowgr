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
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.core.security.model.User;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

/**
 * Json mapping of {@code EntryPoint}.
 * <p>
 * Created by william_montaz on 09/02/2016.
 */
public class EntryPointMappingJson extends EntryPoint {

    @JsonCreator
    public EntryPointMappingJson(@JsonProperty("user") User user,
    							 @JsonProperty("haproxy") String haproxy,
                                 @JsonProperty("hapUser") String hapUser,
                                 @JsonProperty("hapVersion") String hapVersion,
                                 @JsonProperty("bindingId") int bindingId,
                                 @JsonProperty("frontends") Set<EntryPointFrontendMappingJson> frontends,
                                 @JsonProperty("backends") Set<EntryPointBackendMappingJson> backends,
                                 @JsonProperty("context") Map<String, String> context) {
        super(
        		user,
                haproxy,
                hapUser,
                hapVersion,
                bindingId,
                frontends.stream().map(identity()).collect(Collectors.toSet()),
                backends.stream().map(identity()).collect(Collectors.toSet()),
                context
        );
    }

    public EntryPointMappingJson(EntryPoint entryPoint) {
        this(
        		entryPoint.getUser(),
                entryPoint.getHaproxy(),
                entryPoint.getHapUser(),
                entryPoint.getHapVersion(),
                entryPoint.getBindingId(),
                entryPoint.getFrontends().stream().map(EntryPointFrontendMappingJson::new).collect(Collectors.toSet()),
                entryPoint.getBackends().stream().map(EntryPointBackendMappingJson::new).collect(Collectors.toSet()),
                entryPoint.getContext()
        );
    }

}
