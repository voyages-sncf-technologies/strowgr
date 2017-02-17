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
package com.vsct.dt.strowgr.admin.gui.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsct.dt.strowgr.admin.core.event.in.CommitFailureEvent;
import com.vsct.dt.strowgr.admin.core.event.in.CommitSuccessEvent;
import com.vsct.dt.strowgr.admin.core.event.in.RegisterServerEvent;
import com.vsct.dt.strowgr.admin.nsq.NSQ;
import com.vsct.dt.strowgr.admin.nsq.consumer.CommitCompletedTransformer;
import com.vsct.dt.strowgr.admin.nsq.consumer.CommitFailedTransformer;
import com.vsct.dt.strowgr.admin.nsq.consumer.FlowableNSQConsumer;
import com.vsct.dt.strowgr.admin.nsq.consumer.RegisterServerTransformer;
import fr.vsct.dt.nsq.NSQConfig;
import fr.vsct.dt.nsq.lookup.NSQLookup;

public class NSQConsumersFactory {

    private static final String COMMIT_COMPLETED_TOPIC_PREFIX = "commit_completed_";

    private static final String COMMIT_FAILED_TOPIC_PREFIX = "commit_failed_";

    private static final String REGISTER_SERVER_TOPIC = "register_server";

    private final NSQLookup lookup;

    private final NSQConfig nsqConfig;

    private final ObjectMapper objectMapper;

    public NSQConsumersFactory(NSQLookup lookup, NSQConfig nsqConfig, ObjectMapper objectMapper) {
        this.lookup = lookup;
        this.nsqConfig = nsqConfig;
        this.objectMapper = objectMapper;
    }

    public FlowableNSQConsumer<CommitSuccessEvent> buildCommitCompletedConsumer(String id) {
        CommitCompletedTransformer commitCompletedTransformer = new CommitCompletedTransformer(objectMapper);
        return new FlowableNSQConsumer<>(lookup, COMMIT_COMPLETED_TOPIC_PREFIX + id, NSQ.CHANNEL, nsqConfig, commitCompletedTransformer);
    }

    public FlowableNSQConsumer<CommitFailureEvent> buildCommitFailedConsumer(String id) {
        CommitFailedTransformer commitFailedTransformer = new CommitFailedTransformer(objectMapper);
        return new FlowableNSQConsumer<>(lookup, COMMIT_FAILED_TOPIC_PREFIX + id, NSQ.CHANNEL, nsqConfig, commitFailedTransformer);
    }

    public FlowableNSQConsumer<RegisterServerEvent> buildRegisterServerConsumer() {
        RegisterServerTransformer registerServerTransformer = new RegisterServerTransformer(objectMapper);
        return new FlowableNSQConsumer<>(lookup, REGISTER_SERVER_TOPIC, NSQ.CHANNEL, nsqConfig, registerServerTransformer);
    }
}
