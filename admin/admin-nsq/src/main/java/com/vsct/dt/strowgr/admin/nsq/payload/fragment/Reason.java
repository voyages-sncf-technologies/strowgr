package com.vsct.dt.strowgr.admin.nsq.payload.fragment;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Reason {
    public Reason(@JsonProperty("code") int codes, @JsonProperty("shortMessage") String shortMessage, @JsonProperty("longMessage") String longMessage) {
        this.code = codes;
        this.shortMessage = shortMessage;
        this.longMessage = longMessage;
    }

    public enum CODE {
        REGISTER_SERVER(100);

        private int code;

        CODE(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    private final int code;
    private final String shortMessage;
    private final String longMessage;


}
