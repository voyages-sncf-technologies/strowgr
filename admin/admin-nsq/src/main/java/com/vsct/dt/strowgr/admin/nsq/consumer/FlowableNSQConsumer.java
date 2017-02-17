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

import fr.vsct.dt.nsq.NSQConfig;
import fr.vsct.dt.nsq.NSQConsumer;
import fr.vsct.dt.nsq.NSQMessage;
import fr.vsct.dt.nsq.exceptions.NSQException;
import fr.vsct.dt.nsq.lookup.NSQLookup;
import io.netty.channel.EventLoopGroup;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class FlowableNSQConsumer<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableNSQConsumer.class);

    private final String topic;

    private final String channel;

    /**
     * The flowable created by this consumer
     */
    private final Flowable<T> flowable;

    /**
     * Subscription's disposables references for shutdown
     */
    private final List<Disposable> disposables = new ArrayList<>();

    FlowableNSQConsumer(NSQLookup lookup, String topic, String channel, NSQConfig config) {
        this.topic = topic;
        this.channel = channel;

        this.flowable = Flowable
                .<NSQMessage>create(emitter -> {

                    NSQConsumer consumer = new NSQConsumer(lookup, topic, channel, emitter::onNext, config, this::onError);
                    consumer.setLookupPeriod(10 * 1000);
                    consumer.setMessagesPerBatch(10);

                    //We tell the NSQConsumer to use its own eventloop as the executor for message handling
                    //This prevent the use of the default cachedThreadPool, which spawns a huge amount of threads when a lot of messages arrives
                    //The only thing to take care of is that message transformation should never be blocking the event queue
                    //This has to be taken care of by child classes
                    EventLoopGroup eventLoopGroup = config.getEventLoopGroup();
                    consumer.setExecutor(eventLoopGroup);

                    consumer.start();

                    LOGGER.info("new subscription for FlowableNSQConsumer on topic {}, channel {}", topic, channel);

                    emitter.setDisposable(new Disposable() {
                        @Override
                        public void dispose() {
                            consumer.shutdown();
                        }

                        @Override
                        public boolean isDisposed() {
                            return eventLoopGroup.isShutdown();
                        }
                    });

                }, BackpressureStrategy.BUFFER)
                .map(this::transformSafe)
                .filter(Optional::isPresent) // Keep elements which passed transformation
                .map(Optional::get)
                .publish() // do not start flowable immediately
                .autoConnect(1, disposables::add); // start flowable on first subscription and keep subscription's disposable references
    }

    public Flowable<T> flowable() {
        return flowable;
    }

    private Optional<T> transformSafe(NSQMessage nsqMessage) {
        try {
            return Optional.ofNullable(transform(nsqMessage));
        } catch (Exception e) {
            LOGGER.error("can't deserialize the payload of message at {}, id={}, payload={}", nsqMessage.getTimestamp(), new String(nsqMessage.getId()), new String(nsqMessage.getMessage()), e);
            return Optional.empty();
        } finally {
            nsqMessage.finished();
        }
    }

    protected abstract T transform(NSQMessage nsqMessage) throws Exception;

    private void onError(NSQException e) {
        LOGGER.error("Following error occurred while consuming messages on topic {}, channel {}", topic, channel, e);
    }

    /**
     * Shutdown this consumer and all subscriptions.
     */
    public void shutdown() {
        LOGGER.info("Stopping FlowableNSQConsumer on topic {}, channel {}", topic, channel);
        disposables.forEach(Disposable::dispose);
    }

}
