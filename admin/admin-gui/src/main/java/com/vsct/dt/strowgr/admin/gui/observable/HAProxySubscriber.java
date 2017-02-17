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
package com.vsct.dt.strowgr.admin.gui.observable;

import com.vsct.dt.strowgr.admin.nsq.consumer.FlowableNSQConsumer;
import io.dropwizard.lifecycle.Managed;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.subscribers.DefaultSubscriber;
import org.reactivestreams.Subscriber;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * This class subscribes to HAProxyActions: creation or deletion and respectively creates or removes a dedicated NSQConsumer.
 * Events from each NSQConsumers are then forwarded to a given subscriber.
 *
 * @param <U> The type of the commit event to create subscription for
 */
public class HAProxySubscriber<U> extends DefaultSubscriber<HAProxyPublisher.HAProxyAction> implements Managed {

    private final Map<String, FlowableNSQConsumer<U>> nsqConsumers = new HashMap<>();

    private final Function<String, FlowableNSQConsumer<U>> nsqConsumerBuilder;

    private final Subscriber<U> subscriber;

    public HAProxySubscriber(Function<String, FlowableNSQConsumer<U>> nsqConsumerBuilder, Subscriber<U> subscriber) {
        this.nsqConsumerBuilder = nsqConsumerBuilder;
        this.subscriber = subscriber;
    }

    private void createNSQConsumer(String id) {
        FlowableNSQConsumer<U> flowableNSQConsumer = nsqConsumerBuilder.apply(id);
        nsqConsumers.put(id, flowableNSQConsumer);
        flowableNSQConsumer.flowable().subscribe(subscriber);
    }

    private void deleteNSQConsumer(String id) {
        FlowableNSQConsumer flowableNSQConsumer = nsqConsumers.remove(id);
        Optional.ofNullable(flowableNSQConsumer).ifPresent(FlowableNSQConsumer::shutdown);
    }

    @Override
    public void onComplete() {
        //Do nothing, we still want to consume events
    }

    @Override
    public void onError(Throwable e) {
        throw Exceptions.propagate(e);
    }

    @Override
    public void onNext(HAProxyPublisher.HAProxyAction action) {
        if (action.isRegistration()) {
            createNSQConsumer(action.getId());
        } else {
            deleteNSQConsumer(action.getId());
        }
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() {
        Iterator<Map.Entry<String, FlowableNSQConsumer<U>>> iterator = nsqConsumers.entrySet().iterator();
        iterator.forEachRemaining(entry -> {
            entry.getValue().shutdown();
            iterator.remove();
        });
    }

}
