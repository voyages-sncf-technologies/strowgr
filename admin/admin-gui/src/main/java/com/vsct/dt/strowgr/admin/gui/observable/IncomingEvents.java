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

import com.vsct.dt.strowgr.admin.core.event.in.CommitFailureEvent;
import com.vsct.dt.strowgr.admin.core.event.in.CommitSuccessEvent;
import com.vsct.dt.strowgr.admin.core.event.in.RegisterServerEvent;
import com.vsct.dt.strowgr.admin.gui.factory.NSQConsumersFactory;
import com.vsct.dt.strowgr.admin.nsq.consumer.FlowableNSQConsumer;
import com.vsct.dt.strowgr.admin.nsq.consumer.RegisterServerConsumer;
import io.dropwizard.lifecycle.Managed;
import io.reactivex.Flowable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.UnicastProcessor;
import io.reactivex.subscribers.DefaultSubscriber;
import org.reactivestreams.Subscriber;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class IncomingEvents implements Managed {

    private final FlowableProcessor<CommitSuccessEvent> commitSuccessEventProcessor = UnicastProcessor.<CommitSuccessEvent>create().toSerialized();

    private final FlowableProcessor<CommitFailureEvent> commitFailureEventProcessor = UnicastProcessor.<CommitFailureEvent>create().toSerialized();

    private final RegisterServerConsumer registerServerConsumer;

    private final HAProxyActionToCommitEventConsumer<CommitSuccessEvent> haProxyActionToCommitSuccessConsumer;

    private final HAProxyActionToCommitEventConsumer<CommitFailureEvent> haProxyActionToCommitFailureConsumer;

    public IncomingEvents(Flowable<HAProxyPublisher.HAProxyAction> haProxyActionsFlowable, NSQConsumersFactory consumersFactory) {
        this.haProxyActionToCommitSuccessConsumer = new HAProxyActionToCommitEventConsumer<>(consumersFactory::buildCommitCompletedConsumer, commitSuccessEventProcessor);
        this.haProxyActionToCommitFailureConsumer = new HAProxyActionToCommitEventConsumer<>(consumersFactory::buildCommitFailedConsumer, commitFailureEventProcessor);
        this.registerServerConsumer = consumersFactory.buildRegisterServerConsumer();

        haProxyActionsFlowable.subscribe(this.haProxyActionToCommitSuccessConsumer);
        haProxyActionsFlowable.subscribe(this.haProxyActionToCommitFailureConsumer);
    }

    public Flowable<CommitSuccessEvent> commitSuccessEventFlowable() {
        return commitSuccessEventProcessor;
    }

    public Flowable<CommitFailureEvent> commitFailureEventFlowable() {
        return commitFailureEventProcessor;
    }

    public Flowable<RegisterServerEvent> registerServerEventFlowable() {
        return registerServerConsumer.flowable();
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        haProxyActionToCommitSuccessConsumer.shutdownConsumers();
        haProxyActionToCommitFailureConsumer.shutdownConsumers();
        registerServerConsumer.shutdown();
    }

    /**
     * This class subscribes to HAProxyActions: creation or deletion and respectively creates or removes a dedicated NSQConsumer.
     * Events from each NSQConsumers are then forwarded to a given commit event subscriber.
     *
     * @param <U> The type of the commit event to create subscription for
     */
    static class HAProxyActionToCommitEventConsumer<U> extends DefaultSubscriber<HAProxyPublisher.HAProxyAction> {

        private final Map<String, FlowableNSQConsumer<U>> commitEventNSQConsumers = new HashMap<>();

        private final Function<String, FlowableNSQConsumer<U>> commitEventNSQConsumerBuilder;

        private final Subscriber<U> commitEventSubscriber;

        HAProxyActionToCommitEventConsumer(Function<String, FlowableNSQConsumer<U>> commitEventNSQConsumerBuilder, Subscriber<U> commitEventSubscriber) {
            this.commitEventNSQConsumerBuilder = commitEventNSQConsumerBuilder;
            this.commitEventSubscriber = commitEventSubscriber;
        }

        private void createCommitEventConsumer(String id) {
            FlowableNSQConsumer<U> flowableNSQConsumer = commitEventNSQConsumerBuilder.apply(id);
            commitEventNSQConsumers.put(id, flowableNSQConsumer);
            flowableNSQConsumer.flowable().subscribe(commitEventSubscriber);
        }

        private void deleteCommitEventConsumer(String id) {
            FlowableNSQConsumer flowableNSQConsumer = commitEventNSQConsumers.remove(id);
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
                createCommitEventConsumer(action.getId());
            } else {
                deleteCommitEventConsumer(action.getId());
            }
        }

        void shutdownConsumers() {
            Iterator<Map.Entry<String, FlowableNSQConsumer<U>>> iterator = commitEventNSQConsumers.entrySet().iterator();
            iterator.forEachRemaining(entry -> {
                entry.getValue().shutdown();
                iterator.remove();
            });
        }
    }

}
