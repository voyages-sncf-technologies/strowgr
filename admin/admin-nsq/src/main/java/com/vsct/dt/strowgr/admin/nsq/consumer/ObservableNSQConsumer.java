package com.vsct.dt.strowgr.admin.nsq.consumer;

import com.github.brainlag.nsq.NSQConfig;
import com.github.brainlag.nsq.NSQConsumer;
import com.github.brainlag.nsq.NSQMessage;
import com.github.brainlag.nsq.lookup.NSQLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.AsyncEmitter;
import rx.Observable;
import rx.exceptions.Exceptions;

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
 */
public abstract class ObservableNSQConsumer<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObservableNSQConsumer.class);

    private final Observable<NSQMessage> observable;

    /* Since we can pass the callback to NSQConsumer only through its constructor we must
     * delay enclose its creation in the Observable construction which means it will be only when some subscriber
     * subscribes to the observable
     * We keep reference to consumer and to emitter to allow shutdown
     * and emission of onComplete. Make these reference volatile since they can be set in a different thread
     */
    private volatile NSQConsumer              consumer;
    private volatile AsyncEmitter<NSQMessage> emitter;

    public ObservableNSQConsumer(NSQLookup lookup, String topic, String channel) {
        observable = Observable.fromEmitter(emitter -> {
            this.emitter = emitter;

            consumer = new NSQConsumer(lookup, topic, channel, emitter::onNext, new NSQConfig(), emitter::onError);

            consumer.start();

            emitter.setCancellation(() -> consumer.shutdown());

        }, AsyncEmitter.BackpressureMode.BUFFER);
    }

    public Observable<T> observe() {
        return observable.map(this::transformSafe);
    }

    private T transformSafe(NSQMessage nsqMessage) {
        try {
            return transform(nsqMessage);
        } catch (Exception e) {
            LOGGER.error("can't deserialize the payload of message at " + nsqMessage.getTimestamp() + ", id=" + new String(nsqMessage.getId()) + ", payload=" + new String(nsqMessage.getMessage()), e);
            throw Exceptions.propagate(e);
        } finally {
            nsqMessage.finished();
        }
    }

    protected abstract T transform(NSQMessage nsqMessage) throws Exception;

    /**
     * Shutsdown this consumer and advice subscriber
     */
    public void shutdown() {
        if (consumer != null) {
            consumer.shutdown();
        }
        if (emitter != null) {
            emitter.onCompleted();
        }
    }

}
