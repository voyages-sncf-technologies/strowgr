package com.vsct.dt.strowgr.admin.gui.mapping.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HaproxyMappingJson {

    private final String name;
    private final String vip;
    private final String platform;
    private final boolean disabled;

    @JsonCreator
    public HaproxyMappingJson(@JsonProperty("name") String name,
                              @JsonProperty("vip") String vip,
                              @JsonProperty("platform") String platform,
                              @JsonProperty("disabled") boolean disabled) {
        this.name = name;
        this.vip = vip;
        this.platform = platform;
        this.disabled = disabled;
    }

    public String getName() {
        return name;
    }

    public String getVip() {
        return vip;
    }

    public String getPlatform() {
        return platform;
    }

    public boolean getDisabled() {
        return disabled;
    }
}
