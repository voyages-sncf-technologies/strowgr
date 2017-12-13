package com.vsct.dt.strowgr.admin.nsq.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.strowgr.admin.nsq.payload.fragment.Header;
import com.vsct.dt.strowgr.admin.nsq.payload.fragment.Reason;

public class ErrorRaised {
    private final Header header;
    private final Reason reason;

    public ErrorRaised(@JsonProperty("header") Header header, @JsonProperty("reason") Reason reason) {
        this.header = header;
        this.reason = reason;
    }

    public Header getHeader() {
        return header;
    }

    public Reason getReason() {
        return reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ErrorRaised that = (ErrorRaised) o;

        if (header != null ? !header.equals(that.header) : that.header != null) return false;
        return reason != null ? reason.equals(that.reason) : that.reason == null;
    }

    @Override
    public int hashCode() {
        int result = header != null ? header.hashCode() : 0;
        result = 31 * result + (reason != null ? reason.hashCode() : 0);
        return result;
    }
}
