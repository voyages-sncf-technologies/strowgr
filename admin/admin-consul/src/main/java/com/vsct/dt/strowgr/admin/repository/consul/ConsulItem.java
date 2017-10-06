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
package com.vsct.dt.strowgr.admin.repository.consul;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Base64;

public class ConsulItem<T> {

    private final int lockIndex;
    private final String key;
    private final long flags;
    private final String value;
    private final long createIndex;
    private final long modifyIndex;

    @JsonCreator
    public ConsulItem(@JsonProperty("LockIndex") int lockIndex,
                      @JsonProperty("Key") String key,
                      @JsonProperty("Flags") Long flags,
                      @JsonProperty(value = "Value", defaultValue = "{}") String value,
                      @JsonProperty("CreateIndex") Long createIndex,
                      @JsonProperty("ModifyIndex") Long modifyIndex) {
        this.lockIndex = lockIndex;
        this.key = key;
        this.flags = flags;
        this.value = value;
        this.createIndex = createIndex;
        this.modifyIndex = modifyIndex;
    }

    T value(ObjectMapper mapper) {
        try {
            return mapper.readValue(Base64.getDecoder().decode(value), new TypeReference<T>() {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    String valueFromBase64() {
        return new String(Base64.getDecoder().decode(value));
    }

    public int getLockIndex() {
        return lockIndex;
    }

    public String getKey() {
        return key;
    }

    public long getFlags() {
        return flags;
    }

    public String getValue() {
        return value;
    }

    public long getCreateIndex() {
        return createIndex;
    }

    public long getModifyIndex() {
        return modifyIndex;
    }

    @Override
    public String toString() {
        return "ConsulItem{" +
                "lockIndex=" + lockIndex +
                ", key='" + key + '\'' +
                ", flags=" + flags +
                ", value='" + value + '\'' +
                ", createIndex=" + createIndex +
                ", modifyIndex=" + modifyIndex +
                '}';
    }
}