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

package com.vsct.dt.strowgr.admin.nsq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQMessage;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.vsct.dt.strowgr.admin.core.event.in.CommitFailureEvent;
import com.vsct.dt.strowgr.admin.nsq.NSQ;
import com.vsct.dt.strowgr.admin.nsq.payload.CommitFailed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This consumer listens to the commit_failed events for a specific haproxy
 */
public class CommitFailedConsumer extends ObservableNSQConsumer<CommitFailureEvent> {

    private static final String TOPIC_PREFIX = "commit_failed_";

    private final ObjectMapper objectMapper;

    public CommitFailedConsumer(NSQLookup lookup, String haproxy, ObjectMapper objectMapper) {
        super(lookup, TOPIC_PREFIX + haproxy, NSQ.CHANNEL);
        this.objectMapper = objectMapper;
    }

    @Override
    protected CommitFailureEvent transform(NSQMessage nsqMessage) throws Exception {
        CommitFailed payload = objectMapper.readValue(nsqMessage.getMessage(), CommitFailed.class);
        return new CommitFailureEvent(
                payload.getHeader().getCorrelationId(),
                new EntryPointKeyVsctImpl(
                        payload.getHeader().getApplication(),
                        payload.getHeader().getPlatform())
        );
    }
}
