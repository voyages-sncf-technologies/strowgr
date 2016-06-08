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
    private final int createIndex;
    private final int modifyIndex;

    @JsonCreator
    public ConsulItem(@JsonProperty("LockIndex") int lockIndex,
                      @JsonProperty("Key") String key,
                      @JsonProperty("Flags") Long flags,
                      @JsonProperty(value = "Value",defaultValue = "{}") String value,
                      @JsonProperty("CreateIndex") Integer createIndex,
                      @JsonProperty("ModifyIndex") Integer modifyIndex) {
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

    public int getCreateIndex() {
        return createIndex;
    }

    public int getModifyIndex() {
        return modifyIndex;
    }

}