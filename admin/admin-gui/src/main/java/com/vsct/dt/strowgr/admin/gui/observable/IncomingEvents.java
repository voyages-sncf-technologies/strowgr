package com.vsct.dt.strowgr.admin.gui.observable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQConfig;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.vsct.dt.strowgr.admin.core.event.in.CommitFailureEvent;
import com.vsct.dt.strowgr.admin.core.event.in.CommitSuccessEvent;
import com.vsct.dt.strowgr.admin.core.event.in.RegisterServerEvent;
import com.vsct.dt.strowgr.admin.gui.factory.NSQConsumersFactory;
import com.vsct.dt.strowgr.admin.nsq.consumer.CommitCompletedConsumer;
import com.vsct.dt.strowgr.admin.nsq.consumer.CommitFailedConsumer;
import com.vsct.dt.strowgr.admin.nsq.consumer.ObservableNSQConsumer;
import com.vsct.dt.strowgr.admin.nsq.consumer.RegisterServerConsumer;
import rx.Observable;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;
import rx.subjects.UnicastSubject;

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

    private final UnicastSubject<CommitSuccessEvent> commitSuccessEventSubject = UnicastSubject.create();
    private final UnicastSubject<CommitFailureEvent> commitFailureEventSubject = UnicastSubject.create();

    private final RegisterServerConsumer registerServerConsumer;

    private final EventConsumersHandler<CommitCompletedConsumer> commitCompletedConsumerHandler;
    private final EventConsumersHandler<CommitFailedConsumer>    commitFailedConsumerHandler;

    public static IncomingEvents watch(Observable<ManagedHaproxy.HaproxyAction> actionsObservable, NSQConsumersFactory consumersFactory){
        return new IncomingEvents(actionsObservable, consumersFactory);
    }

    private IncomingEvents(Observable<ManagedHaproxy.HaproxyAction> actionsObservable, NSQConsumersFactory consumersFactory) {
        this.commitCompletedConsumerHandler = new EventConsumersHandler<>(commitSuccessEventSubject, id -> consumersFactory.buildCommitCompletedConsumer(id));
        this.commitFailedConsumerHandler = new EventConsumersHandler<>(commitFailureEventSubject, id -> consumersFactory.buildCommitFailedConsumer(id));
        this.registerServerConsumer = consumersFactory.buildRegisterServerConsumer();

        actionsObservable.subscribe(this.commitCompletedConsumerHandler);
        actionsObservable.subscribe(this.commitFailedConsumerHandler);
    }

    public Observable<CommitSuccessEvent> commitSuccessEventObservale() {
        return commitSuccessEventSubject;
    }

    public Observable<CommitFailureEvent> commitFailureEventObservale() {
        return commitFailureEventSubject;
    }

    public Observable<RegisterServerEvent> registerServerEventObservable() {
        return registerServerConsumer.observable();
    }

    public void shutdownConsumers() {
        commitCompletedConsumerHandler.shutdownConsumers();
        commitFailedConsumerHandler.shutdownConsumers();
        registerServerConsumer.shutdown();
    }

    //If the consumer is cancelled there would be a leak because it will still be kept in the map
    //But an ObservableNSQConsumer is very unlikely to happen
    static class EventConsumersHandler<T extends ObservableNSQConsumer> extends Subscriber<ManagedHaproxy.HaproxyAction> {

        private final Map<String, T> consumers = new HashMap<>();
        private final Subject      subject;
        private final Function<String, T> builder;

        EventConsumersHandler(Subject subject, Function<String, T> builder) {
            this.subject = subject;
            this.builder = builder;
        }

        private void createNewCommitEventConsumer(String id) {
            T consumer = builder.apply(id);
            consumers.put(id, consumer);
            consumer.observable().subscribe(subject);
        }

        private void shutdownAndDropCommitEventConsumer(String id) {
            if(consumers.containsKey(id)){
                T consumer = consumers.get(id);
                consumer.shutdown();
                consumers.remove(id);
            }
        }

        @Override
        public void onCompleted() {
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
            }
            else {
                shutdownAndDropCommitEventConsumer(action.getId());
            }
        }

        public void shutdownConsumers() {
            Iterator<Map.Entry<String, T>> it = consumers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, T> e = it.next();
                e.getValue().shutdown();
                it.remove();
            }
        }
    }

}
