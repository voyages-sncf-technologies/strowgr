package com.vsct.dt.strowgr.admin.gui.observable;

import com.vsct.dt.strowgr.admin.core.event.in.CommitFailureEvent;
import com.vsct.dt.strowgr.admin.core.event.in.CommitSuccessEvent;
import com.vsct.dt.strowgr.admin.core.event.in.RegisterServerEvent;
import com.vsct.dt.strowgr.admin.gui.factory.NSQConsumersFactory;
import com.vsct.dt.strowgr.admin.nsq.consumer.CommitCompletedConsumer;
import com.vsct.dt.strowgr.admin.nsq.consumer.CommitFailedConsumer;
import com.vsct.dt.strowgr.admin.nsq.consumer.FlowableNSQConsumer;
import com.vsct.dt.strowgr.admin.nsq.consumer.RegisterServerConsumer;
import io.reactivex.Flowable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.UnicastProcessor;
import io.reactivex.subscribers.DefaultSubscriber;
import org.reactivestreams.Subscriber;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

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
public class IncomingEvents {

    private final FlowableProcessor<CommitSuccessEvent> commitSuccessEventProcessor = UnicastProcessor.<CommitSuccessEvent>create().toSerialized();
    private final FlowableProcessor<CommitFailureEvent> commitFailureEventProcessor = UnicastProcessor.<CommitFailureEvent>create().toSerialized();

    private final RegisterServerConsumer registerServerConsumer;

    private final EventConsumersHandler<CommitCompletedConsumer, CommitSuccessEvent> commitCompletedConsumerHandler;
    private final EventConsumersHandler<CommitFailedConsumer, CommitFailureEvent> commitFailedConsumerHandler;

    public IncomingEvents(Flowable<ManagedHaproxy.HaproxyAction> actionsObservable, NSQConsumersFactory consumersFactory) {
        this.commitCompletedConsumerHandler = new EventConsumersHandler<>(commitSuccessEventProcessor, consumersFactory::buildCommitCompletedConsumer);
        this.commitFailedConsumerHandler = new EventConsumersHandler<>(commitFailureEventProcessor, consumersFactory::buildCommitFailedConsumer);
        this.registerServerConsumer = consumersFactory.buildRegisterServerConsumer();

        actionsObservable.subscribe(this.commitCompletedConsumerHandler);
        actionsObservable.subscribe(this.commitFailedConsumerHandler);
    }

    public Flowable<CommitSuccessEvent> commitSuccessEventFlowable() {
        return commitSuccessEventProcessor;
    }

    public Flowable<CommitFailureEvent> commitFailureEventFlowable() {
        return commitFailureEventProcessor;
    }

    Flowable<RegisterServerEvent> registerServerEventObservable() {
        return registerServerConsumer.flowable();
    }

    public void shutdownConsumers() {
        commitCompletedConsumerHandler.shutdownConsumers();
        commitFailedConsumerHandler.shutdownConsumers();
        registerServerConsumer.shutdown();
    }

    //If the consumer is cancelled there would be a leak because it will still be kept in the map
    //But an FlowableNSQConsumer is very unlikely to be cancelled. This would mean a bigger problem in the app...
    static class EventConsumersHandler<T extends FlowableNSQConsumer<U>, U> extends DefaultSubscriber<ManagedHaproxy.HaproxyAction> {

        private final Map<String, T> consumers = new HashMap<>();
        private final Subscriber<U> subscriber;
        private final Function<String, T> builder;

        EventConsumersHandler(Subscriber<U> subscriber, Function<String, T> builder) {
            this.subscriber = subscriber;
            this.builder = builder;
        }

        private void createNewCommitEventConsumer(String id) {
            T consumer = builder.apply(id);
            consumers.put(id, consumer);
            consumer.flowable().subscribe(subscriber);
        }

        private void shutdownAndDropCommitEventConsumer(String id) {
            if (consumers.containsKey(id)) {
                T consumer = consumers.get(id);
                consumer.shutdown();
                consumers.remove(id);
            }
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
        public void onNext(ManagedHaproxy.HaproxyAction action) {
            if (action.isRegistration()) {
                createNewCommitEventConsumer(action.getId());
            } else {
                shutdownAndDropCommitEventConsumer(action.getId());
            }
        }

        void shutdownConsumers() {
            Iterator<Map.Entry<String, T>> it = consumers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, T> e = it.next();
                e.getValue().shutdown();
                it.remove();
            }
        }
    }

}
