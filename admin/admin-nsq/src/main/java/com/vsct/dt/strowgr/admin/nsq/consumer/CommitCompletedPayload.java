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

package com.vsct.dt.strowgr.admin.nsq.consumer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.strowgr.admin.nsq.Payload;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by william_montaz on 02/02/2016.
 */
public class CommitCompletedPayload extends Payload {

    private final String application;
    private final String platform;

    @JsonCreator
    public CommitCompletedPayload(@JsonProperty("correlationId") String correlationId,
                                  @JsonProperty("timestamp") Long timestamp,
                                  @JsonProperty("application") String application,
                                  @JsonProperty("platform") String platform) {
        super(correlationId, timestamp);
        this.application = checkNotNull(application);
        this.platform = checkNotNull(platform);
    }

    public String getApplication() {
        return application;
    }

    public String getPlatform() {
        return platform;
    }
}
