/*
 *  Copyright (C) 2016 VSCT
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.vsct.strowgr.monitoring.aggregator.cassandra;

import com.github.brainlag.nsq.NSQMessage;
import com.vsct.strowgr.monitoring.aggregator.StringDates;

import java.util.Date;

public class ErrorRecord {

    private final String payload;
    private final Date   timestamp;
    private final String date;
    private final String reason;

    public ErrorRecord(NSQMessage message, String reason) {
        this.date = StringDates.ISO_LOCAL_DATE(message.getTimestamp());
        this.payload = new String(message.getMessage());
        this.timestamp = message.getTimestamp();
        this.reason = reason;
    }

    public String getPayload() {
        return payload;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getDate() {
        return date;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "ErrorRecord{" +
                "payload='" + payload + '\'' +
                ", timestamp=" + timestamp +
                ", date='" + date + '\'' +
                ", reason='" + reason + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ErrorRecord that = (ErrorRecord) o;

        if (payload != null ? !payload.equals(that.payload) : that.payload != null) return false;
        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) return false;
        if (date != null ? !date.equals(that.date) : that.date != null) return false;
        if (reason != null ? !reason.equals(that.reason) : that.reason != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = payload != null ? payload.hashCode() : 0;
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (reason != null ? reason.hashCode() : 0);
        return result;
    }
}
