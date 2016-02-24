package com.vsct.dt.haas.admin.repository.consul;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;

public class ConsulItem<T> {

    @JsonProperty("LockIndex") Integer lockIndex;
    @JsonProperty("Key") String key;
    @JsonProperty("Flags") Integer flags;
    @JsonProperty("Value") String value;
    @JsonProperty("CreateIndex") Integer createIndex;
    @JsonProperty("ModifyIndex") Integer modifyIndex;

    public ConsulItem() {
    }

    public ConsulItem(Integer lockIndex, String key, Integer flags, String value, Integer createIndex, Integer modifyIndex) {
        this.lockIndex = lockIndex;
        this.key = key;
        this.flags = flags;
        this.value = value;
        this.createIndex = createIndex;
        this.modifyIndex = modifyIndex;
    }

    T value(ObjectMapper mapper) throws IOException {
        return mapper.readValue(Base64.getDecoder().decode(value), new TypeReference<T>() {
        });
    }

    public Integer getLockIndex() {
        return lockIndex;
    }

    public void setLockIndex(Integer lockIndex) {
        this.lockIndex = lockIndex;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getFlags() {
        return flags;
    }

    public void setFlags(Integer flags) {
        this.flags = flags;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getCreateIndex() {
        return createIndex;
    }

    public void setCreateIndex(Integer createIndex) {
        this.createIndex = createIndex;
    }

    public Integer getModifyIndex() {
        return modifyIndex;
    }

    public void setModifyIndex(Integer modifyIndex) {
        this.modifyIndex = modifyIndex;
    }
}