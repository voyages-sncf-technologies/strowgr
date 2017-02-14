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
import com.vsct.dt.strowgr.admin.core.event.in.UpdatedEntryPoint;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

/**
 * Created by william_montaz on 12/04/2016.
 */
public class UpdatedEntryPointMappingJson extends UpdatedEntryPoint {

    @JsonCreator
    public UpdatedEntryPointMappingJson(@JsonProperty("hapUser") String hapUser, @JsonProperty("hapVersion") String hapVersion, @JsonProperty("bindingId") int bindingId,
                                        @JsonProperty("context") Map<String, String> context,
                                        @JsonProperty("frontends") Set<UpdatedEntryPointFrontendMappingJson> frontends,
                                        @JsonProperty("backends") Set<UpdatedEntryPointBackendMappingJson> backends) {
        super(bindingId, hapUser, context,
                frontends.stream().map(identity()).collect(Collectors.toSet()),
                backends.stream().map(identity()).collect(Collectors.toSet()), hapVersion);
    }
}
