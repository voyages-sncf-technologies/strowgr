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
import com.github.brainlag.nsq.NSQConsumer;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.vsct.dt.strowgr.admin.core.event.in.CommitSuccessEvent;
import com.vsct.dt.strowgr.admin.nsq.payload.CommitCompleted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.exceptions.Exceptions;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

public class CommitCompletedConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommitCompletedConsumer.class);

    private static final String CHANNEL = "admin";
    private static final String TOPIC_PREFIX = "commit_completed_";

    private final ObservableNSQConsumer nsqConsumer;
    private final Observable<CommitSuccessEvent> observable;

    public CommitCompletedConsumer(NSQLookup lookup, String haproxy, ObjectMapper mapper) {
        nsqConsumer = new ObservableNSQConsumer(lookup, TOPIC_PREFIX + haproxy, CHANNEL);
        observable = nsqConsumer.observe().map(nsqMessage -> {
            try {
                CommitCompleted payload = mapper.readValue(nsqMessage.getMessage(), CommitCompleted.class);
                return new CommitSuccessEvent(
                        payload.getHeader().getCorrelationId(),
                        new EntryPointKeyVsctImpl(
                                payload.getHeader().getApplication(),
                                payload.getHeader().getPlatform()
                        )
                );
            } catch (IOException e) {
                LOGGER.error("can't deserialize the payload:" + Arrays.toString(nsqMessage.getMessage()), e);
                throw Exceptions.propagate(e);
            } finally {
                nsqMessage.finished();
            }
        });
    }

    public Observable<CommitSuccessEvent> observe(){
        return observable;
    }

    public void shutdown() {
        nsqConsumer.shutdown();
    }

}
