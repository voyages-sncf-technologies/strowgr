package com.vsct.dt.strowgr.admin.gui.mapping.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HaproxyMappingJson {
    @JsonProperty("name")
    private String name;
    @JsonProperty("vip")
    private String vip;
    @JsonProperty("platform")
    private String platform;
    @JsonProperty("disabled")
    private String disabled = "false";

    public String getName() {
        return name;
    }

    public String getVip() {
        return vip;
    }

    public String getPlatform() {
        return platform;
    }

    public String getDisabled() {
        return disabled;
    }
}
