package com.vsct.dt.strowgr.admin.nsq.consumer;

import fr.vsct.dt.nsq.NSQConfig;
import fr.vsct.dt.nsq.NSQConsumer;
import fr.vsct.dt.nsq.NSQMessage;
import fr.vsct.dt.nsq.lookup.NSQLookup;
import io.netty.channel.EventLoopGroup;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

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
 * <p>
 * <p>
 * This class is intended to expose an NSQCOnsumer as an RXJava Observable
 * It handles the emission of new messages, complete on shutdown and shutdown on subscriber cancellation
 * The implementing classes just have to implement the transform method, to provided a domain oriented message instead of a raw NSQMessage
 * <p>
 * Note: observers to this flowable should not be blocking, if they do, they can block the eventloop
 * if observers needs to block, when should use the observeOn method of the flowable to choose a different scheduler
 */
public abstract class FlowableNSQConsumer<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableNSQConsumer.class);

    private final String topic;
    private final String channel;

    /* The flowable created by this consumer */
    private final Flowable<T> flowable;

    /*
     * We keep reference to consumer and to emitter to allow shutdown
     * and emission of onComplete. Make these reference volatile since they can be set in a different thread
     */
    private volatile NSQConsumer consumer;
    private volatile FlowableEmitter<NSQMessage> emitter;

    FlowableNSQConsumer(NSQLookup lookup, String topic, String channel, NSQConfig config) {
        this.topic = topic;
        this.channel = channel;

        Flowable<NSQMessage> flowable = Flowable
                .create(emitter -> {
                    this.emitter = emitter;
                    consumer = new NSQConsumer(lookup, topic, channel, emitter::onNext, config, emitter::onError);
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

                }, BackpressureStrategy.BUFFER);

        this.flowable = flowable
                .map(this::transformSafe)
                .filter(Optional::isPresent)
                .doOnError(throwable -> LOGGER.error("consumer error on topic " + topic, throwable))
                .map(Optional::get) //We don't want to stop the flowable on error
                .publish().autoConnect(); //Use publish/refCount for two reasons : NSQConsumer will be created only once. Observable will auto start
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

    /**
     * Shutsdown this consumer and advice subscriber
     */
    public void shutdown() {
        LOGGER.info("stopping FlowableNSQConsumer on topic {}, channel {}", topic, channel);
        if (consumer != null) {
            consumer.shutdown();
        }
        if (emitter != null) {
            emitter.onComplete();
        }
    }

}