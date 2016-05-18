package com.vsct.strowgr.monitoring.aggregator.cassandra;

import com.vsct.strowgr.monitoring.aggregator.StringDates;
import com.vsct.strowgr.monitoring.aggregator.nsq.NsqEventHeader;

import java.util.Date;
import java.util.UUID;

public class ParsedPayload {

    private final String id;
    private final String date;
    private final String eventName;
    private final Date   eventTimestamp;
    private final UUID   correlationId;
    private final String haproxyId;
    private final String payload;

    public ParsedPayload(NsqEventHeader header, String haproxyId, String eventName, String payload) {
        this.id = header.getApplication() + "/" + header.getPlatform();
        this.eventTimestamp = header.getTimestamp();
        this.eventName = eventName;
        this.correlationId = header.getCorrelationId();
        this.haproxyId = haproxyId;
        this.payload = payload;
        this.date = StringDates.ISO_LOCAL_DATE(this.eventTimestamp);
    }

    public String getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public String getEventName() {
        return eventName;
    }

    public Date getEventTimestamp() {
        return eventTimestamp;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public String getHaproxyId() {
        return haproxyId;
    }

    public String getPayload() {
        return payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParsedPayload that = (ParsedPayload) o;

        if (eventTimestamp != that.eventTimestamp) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (date != null ? !date.equals(that.date) : that.date != null) return false;
        if (eventName != null ? !eventName.equals(that.eventName) : that.eventName != null) return false;
        if (correlationId != null ? !correlationId.equals(that.correlationId) : that.correlationId != null)
            return false;
        if (haproxyId != null ? !haproxyId.equals(that.haproxyId) : that.haproxyId != null) return false;
        if (payload != null ? !payload.equals(that.payload) : that.payload != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (eventName != null ? eventName.hashCode() : 0);
        result = 31 * result + (eventTimestamp != null ? eventTimestamp.hashCode() : 0);
        result = 31 * result + (correlationId != null ? correlationId.hashCode() : 0);
        result = 31 * result + (haproxyId != null ? haproxyId.hashCode() : 0);
        result = 31 * result + (payload != null ? payload.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CassandraEvent{" +
                "id='" + id + '\'' +
                ", date='" + date + '\'' +
                ", eventName='" + eventName + '\'' +
                ", eventTimestamp=" + eventTimestamp +
                ", correlationId='" + correlationId + '\'' +
                ", haproxyId='" + haproxyId + '\'' +
                ", payload='" + payload + '\'' +
                '}';
    }
}
