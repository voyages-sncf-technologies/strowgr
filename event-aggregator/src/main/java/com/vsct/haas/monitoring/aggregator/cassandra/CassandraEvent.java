package com.vsct.haas.monitoring.aggregator.cassandra;

import com.vsct.haas.monitoring.aggregator.nsq.NsqEventHeader;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CassandraEvent {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int MILLISEC_PER_SEC = 1000;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int SECONDS_PER_HOUR = 60 * SECONDS_PER_MINUTE;
    private static final int HOURS_PER_DAY = 24;
    private static final int SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY;
    private static final int MILLISEC_PER_DAY = MILLISEC_PER_SEC * SECONDS_PER_DAY;

    private final String id;
    private final String date;
    private final String eventName;
    private final long   eventTimestamp;
    private final String correlationId;
    private final String haproxyId;
    private final String payload;

    public CassandraEvent(NsqEventHeader header, String haproxyId, String eventName, String payload) {
        this.id = header.getApplication() + "/" + header.getPlatform();
        this.eventTimestamp = header.getTimestamp();
        this.eventName = eventName;
        this.correlationId = header.getCorrelationId();
        this.haproxyId = haproxyId;
        this.payload = payload;
        this.date = LocalDate.ofEpochDay(header.getTimestamp() / MILLISEC_PER_DAY).format(dateTimeFormatter);
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

    public long getEventTimestamp() {
        return eventTimestamp;
    }

    public String getCorrelationId() {
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

        CassandraEvent that = (CassandraEvent) o;

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
        result = 31 * result + (int) (eventTimestamp ^ (eventTimestamp >>> 32));
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
