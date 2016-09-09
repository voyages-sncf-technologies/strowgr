package com.vsct.dt.strowgr.admin.gui.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQConfig;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.vsct.dt.strowgr.admin.core.event.in.RegisterServerEvent;
import com.vsct.dt.strowgr.admin.nsq.consumer.CommitCompletedConsumer;
import com.vsct.dt.strowgr.admin.nsq.consumer.CommitFailedConsumer;
import com.vsct.dt.strowgr.admin.nsq.consumer.RegisterServerConsumer;
import rx.Observable;

/**
 * ~  Copyright (C) 2016 VSCT
 * ~
 * ~  Licensed under the Apache License, Version 2.0 (the "License");
 * ~  you may not use this file except in compliance with the License.
 * ~  You may obtain a copy of the License at
 * ~
 * ~   http://www.apache.org/licenses/LICENSE-2.0
 * ~
 * ~  Unless required by applicable law or agreed to in writing, software
 * ~  distributed under the License is distributed on an "AS IS" BASIS,
 * ~  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * ~  See the License for the specific language governing permissions and
 * ~  limitations under the License.
 * ~
 */
public class NSQConsumersFactory {

    private final NSQLookup    lookup;
    private final NSQConfig    nsqConfig;
    private final ObjectMapper objectMapper;

    public static NSQConsumersFactory make(NSQLookup lookup, NSQConfig nsqConfig, ObjectMapper objectMapper) {
        return new NSQConsumersFactory(lookup, nsqConfig, objectMapper);
    }

    protected NSQConsumersFactory(NSQLookup lookup, NSQConfig nsqConfig, ObjectMapper objectMapper) {
        this.lookup = lookup;
        this.nsqConfig = nsqConfig;
        this.objectMapper = objectMapper;
    }

    public CommitCompletedConsumer buildCommitCompletedConsumer(String id) {
        return new CommitCompletedConsumer(lookup, id, objectMapper, nsqConfig);
    }

    public CommitFailedConsumer buildCommitFailedConsumer(String id) {
        return new CommitFailedConsumer(lookup, id, objectMapper, nsqConfig);
    }

    public RegisterServerConsumer buildRegisterServerConsumer() {
        return new RegisterServerConsumer(lookup, objectMapper, nsqConfig);
    }
}
