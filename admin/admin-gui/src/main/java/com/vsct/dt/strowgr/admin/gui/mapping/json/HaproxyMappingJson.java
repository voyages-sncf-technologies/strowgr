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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HaproxyMappingJson {

    @NotEmpty
    private final String              name;
    @NotNull
    private final Map<Integer, String> bindings;
    @NotEmpty
    private final String              platform;
    @NotNull
    private final Boolean             autoreload;

    
    @JsonCreator
    public HaproxyMappingJson(@JsonProperty("name") String name,
                              @JsonProperty("bindings") Map<Integer, String> bindings,
                              @JsonProperty("platform") String platform,
                              @JsonProperty("autoreload") boolean autoreload) {
    	
    	this.name = name;
        this.bindings = bindings;
        this.platform = platform;
        this.autoreload = autoreload;
    }

    public String getName() {
        return name;
    }

    public Map<Integer, String> getBindings() {
        return bindings;
    }

    public String getPlatform() {
        return platform;
    }

    public boolean getAutoreload() {
        return autoreload;
    }
}
