package com.vsct.haas.monitoring.aggregator.nsq;

import java.io.IOException;

public class UnavailableNsqException extends Exception {
    public UnavailableNsqException(Exception e) {
        super(e);
    }
}
