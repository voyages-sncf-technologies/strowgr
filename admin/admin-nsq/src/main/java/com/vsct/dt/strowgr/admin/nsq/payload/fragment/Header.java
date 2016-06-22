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

import static com.google.common.base.Preconditions.checkNotNull;

public class Header {
    @JsonProperty("correlationId")
    private String correlationId;
    @JsonProperty("application")
    private String application;
    @JsonProperty("platform")
    private String platform;
    @JsonProperty("timestamp")
    private Long timestamp;
    @JsonProperty("source")
    private String source;

    @JsonCreator
    public Header() {
    }

    public Header(String correlationId, String application, String platform) {
        this.correlationId = checkNotNull(correlationId);
        this.application = checkNotNull(application);
        this.platform = checkNotNull(platform);
        this.timestamp = System.currentTimeMillis();
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
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

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
