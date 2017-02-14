/*
 * Copyright (C) 2016 VSCT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vsct.dt.strowgr.admin.nsq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.vsct.dt.nsq.NSQConfig;
import fr.vsct.dt.nsq.NSQMessage;
import fr.vsct.dt.nsq.lookup.NSQLookup;
import com.vsct.dt.strowgr.admin.core.event.in.CommitSuccessEvent;
import com.vsct.dt.strowgr.admin.nsq.NSQ;
import com.vsct.dt.strowgr.admin.nsq.payload.CommitCompleted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This consumer listens to the commit_completed events for a specific haproxy
 */
public class CommitCompletedConsumer extends FlowableNSQConsumer<CommitSuccessEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommitCompletedConsumer.class);

    private static final String TOPIC_PREFIX = "commit_completed_";

    private final ObjectMapper objectMapper;

    public CommitCompletedConsumer(NSQLookup lookup, String haproxy, ObjectMapper objectMapper, NSQConfig config) {
        super(lookup, TOPIC_PREFIX + haproxy, NSQ.CHANNEL, config);
        this.objectMapper = objectMapper;
    }

    @Override
    protected CommitSuccessEvent transform(NSQMessage nsqMessage) throws Exception {
        CommitCompleted payload = objectMapper.readValue(nsqMessage.getMessage(), CommitCompleted.class);

        LOGGER.debug("received an new CommitSuccesEvent with cid {}", payload.getHeader().getCorrelationId());

        return new CommitSuccessEvent(
                payload.getHeader().getCorrelationId(),
                new EntryPointKeyVsctImpl(
                        payload.getHeader().getApplication(),
                        payload.getHeader().getPlatform()
                )
        );
    }

}
