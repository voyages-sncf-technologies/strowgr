package com.vsct.dt.strowgr.admin.gui.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.nsq.NSQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * ~  Copyright (C) 2016 VSCT
 * ~
 * ~  Licensed under the Apache License, Version 2.0 (the "License");
 * ~  you may not use this file except in compliance with the License.
 * ~  You may obtain a copy of the License at
 * ~
 * ~   http://www.apache.org/licenses/LICENSE-2.0
 * ~
 * ~  Unless required by applicable law or agreed to in writing, software
 * ~  distributed under the License is distributed on an "AS IS" BASIS,
 * ~  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * ~  See the License for the specific language governing permissions and
 * ~  limitations under the License.
 * ~
 */
public class NSQCompressionFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(NSQConfigFactory.class);

    @Nullable
    private String  type;
    @Min(0)
    @Max(9)
    @Nullable
    private Integer deflateLevel = 0;

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("level")
    public Integer getDeflateLevel() {
        if(!getCompression().equals(NSQConfig.Compression.DEFLATE)){
            LOGGER.warn("deflateLevel property should only be used with deflate compression. Property is ignored.");
            //Not nice but NSQConfig class uses null check on that value,
            //hence we have to return null to it
            return null;
        }
        return deflateLevel;
    }

    @JsonProperty("level")
    public void setDeflateLevel(Integer deflateLevel) {
        this.deflateLevel = deflateLevel;
    }

    public NSQConfig.Compression getCompression() {
        if(type == null){
            return NSQConfig.Compression.NO_COMPRESSION;
        }
        if ("snappy".equals(type)) {
            return NSQConfig.Compression.SNAPPY;
        }
        if ("deflate".equals(type)) {
            return NSQConfig.Compression.DEFLATE;
        }
        LOGGER.warn("Compression {} is not available, default to no compression. Allowed values : deflate|snappy");
        return NSQConfig.Compression.NO_COMPRESSION;
    }
}
