package com.vsct.dt.strowgr.admin.core.event;

public enum ErrorCodes {
    REGISTER_SERVER(100, "can't register a server.");

    private int code;
    private String description;

    ErrorCodes(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }


    public String getDescription() {
        return description;
    }
}
