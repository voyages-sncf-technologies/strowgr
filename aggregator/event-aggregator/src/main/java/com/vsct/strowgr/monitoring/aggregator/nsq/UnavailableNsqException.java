package com.vsct.strowgr.monitoring.aggregator.nsq;

public class UnavailableNsqException extends Exception {
    public UnavailableNsqException(Exception e) {
        super(e);
    }
}
