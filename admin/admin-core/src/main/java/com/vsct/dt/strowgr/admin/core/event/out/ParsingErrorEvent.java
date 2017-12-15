package com.vsct.dt.strowgr.admin.core.event.out;

public class ParsingErrorEvent {
    private final String correlationId;
    private final String shortMessage;
    private final String longMessage;

    public ParsingErrorEvent(String correlationId, String shortMessage, String longMessage) {
        this.correlationId = correlationId;
        this.shortMessage = shortMessage;
        this.longMessage = longMessage;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getShortMessage() {
        return shortMessage;
    }

    public String getLongMessage() {
        return longMessage;
    }
}
