package com.vsct.dt.strowgr.admin.nsq.payload.fragment;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Error {
    public Error(@JsonProperty("code") int codes, @JsonProperty("shortMessage") String shortMessage, @JsonProperty("longMessage") String longMessage) {
        this.errorCode = codes;
        this.shortMessage = shortMessage;
        this.longMessage = longMessage;
    }

    enum errorCodes {
        REGISTER_SERVER(100);

        private int code;

        errorCodes(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    private final int errorCode;
    private final String shortMessage;
    private final String longMessage;


}
