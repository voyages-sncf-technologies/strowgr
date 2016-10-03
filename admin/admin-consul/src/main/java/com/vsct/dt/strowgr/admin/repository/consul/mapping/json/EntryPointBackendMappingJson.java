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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPointBackend;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Json mapping of {@code EntryPointBackend}.
 *
 * Created by william_montaz on 09/02/2016.
 */
public class EntryPointBackendMappingJson extends EntryPointBackend {

    public EntryPointBackendMappingJson(@JsonProperty("id") String id,
                                        @JsonProperty("servers") Set<EntryPointBackendServerMappingJson> servers,
                                        @JsonProperty("context") Map<String, String> context) {
        super(id, servers.stream().map(Function.identity()).collect(Collectors.toSet()), context);
    }

    public EntryPointBackendMappingJson(EntryPointBackend entryPointBackend){
        this(
                entryPointBackend.getId(),
                entryPointBackend.getServers().stream().map(EntryPointBackendServerMappingJson::new).collect(Collectors.toSet()),
                entryPointBackend.getContext()
        );
    }

}
