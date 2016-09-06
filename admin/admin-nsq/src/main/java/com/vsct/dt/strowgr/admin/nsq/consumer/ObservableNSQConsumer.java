package com.vsct.dt.strowgr.admin.nsq.consumer;

import com.github.brainlag.nsq.NSQConfig;
import com.github.brainlag.nsq.NSQConsumer;
import com.github.brainlag.nsq.NSQMessage;
import com.github.brainlag.nsq.lookup.NSQLookup;
import rx.AsyncEmitter;
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
public abstract class ObservableNSQConsumer<T> {

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

        }, AsyncEmitter.BackpressureMode.ERROR);
    }

    public Observable<T> observe() {
        return observable.map(this::transformSafe);
    }

    private T transformSafe(NSQMessage nsqMessage) {
        try {
            return transform(nsqMessage);
        } finally {
            nsqMessage.finished();
        }
    }

    protected abstract T transform(NSQMessage nsqMessage);

    public void shutdown() {
        if (consumer != null) {
            consumer.shutdown();
        }
        if (emitter != null) {
            emitter.onCompleted();
        }
    }

}
