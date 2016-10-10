/*
 * Copyright (C) 2016 VSCT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.vsct.dt.strowgr.admin.nsq.payload.fragment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.strowgr.admin.nsq.NSQ;

import static com.google.common.base.Preconditions.checkNotNull;

public class Header {

    private final String correlationId;
    private final String application;
    private final String platform;
    private final Long   timestamp;
    private final String source;

    @JsonCreator
    public Header(@JsonProperty("correlationId") String correlationId,
                  @JsonProperty("application") String application,
                  @JsonProperty("platform") String platform,
                  @JsonProperty("timestamp") Long timestamp,
                  @JsonProperty("source") String source) {
        this.correlationId = checkNotNull(correlationId);
        this.application = checkNotNull(application);
        this.platform = checkNotNull(platform);
        this.timestamp = timestamp;
        this.source = source;
    }

    public Header(String correlationId, String application, String platform){
        this(correlationId, application, platform, System.currentTimeMillis(), NSQ.SOURCE_NAME);
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getApplication() {
        return application;
    }

    public String getPlatform() {
        return platform;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getSource() {
        return source;
    }
}
