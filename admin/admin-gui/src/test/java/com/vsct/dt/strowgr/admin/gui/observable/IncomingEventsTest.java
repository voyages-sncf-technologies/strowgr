package com.vsct.dt.strowgr.admin.gui.observable;

import com.vsct.dt.strowgr.admin.core.event.in.CommitFailureEvent;
import com.vsct.dt.strowgr.admin.core.event.in.CommitSuccessEvent;
import com.vsct.dt.strowgr.admin.core.event.in.RegisterServerEvent;
import com.vsct.dt.strowgr.admin.gui.factory.NSQConsumersFactory;
import com.vsct.dt.strowgr.admin.nsq.consumer.CommitCompletedConsumer;
import com.vsct.dt.strowgr.admin.nsq.consumer.CommitFailedConsumer;
import com.vsct.dt.strowgr.admin.nsq.consumer.RegisterServerConsumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

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
public class IncomingEventsTest {

    static class NSQConsumersMockFactory extends NSQConsumersFactory {

        NSQConsumersMockFactory() {
            super(null, null, null);
        }

        @Override
        public CommitCompletedConsumer buildCommitCompletedConsumer(String id) {
            return CommitCompletedConsumerMock.create(id);
        }

        @Override
        public CommitFailedConsumer buildCommitFailedConsumer(String id) {
            return CommitFailedConsumerMock.create(id);
        }

        @Override
        public RegisterServerConsumer buildRegisterServerConsumer() {
            return RegisterServerConsumerMock.create("single");
        }
    }

    IncomingEvents                               incomingEvents;
    PublishSubject<ManagedHaproxy.HaproxyAction> actionsObservable;

    @Before
    public void setUp() {
        actionsObservable = PublishSubject.create();
        incomingEvents = IncomingEvents.watch(actionsObservable, new NSQConsumersMockFactory());
    }

    @After
    public void tearDown() {
        CommitCompletedConsumerMock.clear();
        CommitFailedConsumerMock.clear();
    }

    @Test
    public void should_broadcast_commit_success_events_of_new_haproxy() {
        List<String> observedEvents = new ArrayList<>();

        incomingEvents.commitSuccessEventObservale().subscribe(event -> {
            observedEvents.add(event.getCorrelationId());
        });

        actionsObservable.onNext(ManagedHaproxy.HaproxyAction.register("hap1"));

        CommitCompletedConsumerMock.sendEvent("hap1", "1-1");
        CommitCompletedConsumerMock.sendEvent("hap1", "1-2");

        actionsObservable.onNext(ManagedHaproxy.HaproxyAction.register("hap2"));

        CommitCompletedConsumerMock.sendEvent("hap2", "2-1");
        CommitCompletedConsumerMock.sendEvent("hap1", "1-3");
        CommitCompletedConsumerMock.sendEvent("hap2", "2-2");
        CommitCompletedConsumerMock.sendEvent("hap1", "1-4");

        List<String> expected = Arrays.asList("1-1", "1-2", "2-1", "1-3", "2-2", "1-4");
        assertThat(observedEvents, is(expected));
    }

    @Test
    public void should_broadcast_commit_failure_events_of_new_haproxy() {
        List<String> observedEvents = new ArrayList<>();

        incomingEvents.commitFailureEventObservale().subscribe(event -> {
            observedEvents.add(event.getCorrelationId());
        });

        actionsObservable.onNext(ManagedHaproxy.HaproxyAction.register("hap1"));

        CommitFailedConsumerMock.sendEvent("hap1", "1-1");
        CommitFailedConsumerMock.sendEvent("hap1", "1-2");

        actionsObservable.onNext(ManagedHaproxy.HaproxyAction.register("hap2"));

        CommitFailedConsumerMock.sendEvent("hap2", "2-1");
        CommitFailedConsumerMock.sendEvent("hap1", "1-3");
        CommitFailedConsumerMock.sendEvent("hap2", "2-2");
        CommitFailedConsumerMock.sendEvent("hap1", "1-4");

        List<String> expected = Arrays.asList("1-1", "1-2", "2-1", "1-3", "2-2", "1-4");
        assertThat(observedEvents, is(expected));
    }

    @Test
    public void should_not_fail_if_unregister_non_registered_haproxy() {
        actionsObservable.onNext(ManagedHaproxy.HaproxyAction.unregister("hap1"));
    }

    @Test
    public void should_shutdown_unregistered_haproxy() {
        actionsObservable.onNext(ManagedHaproxy.HaproxyAction.register("hap1"));
        actionsObservable.onNext(ManagedHaproxy.HaproxyAction.register("hap2"));

        actionsObservable.onNext(ManagedHaproxy.HaproxyAction.unregister("hap1"));
        actionsObservable.onNext(ManagedHaproxy.HaproxyAction.unregister("hap2"));

        verify(CommitCompletedConsumerMock.get("hap1")).shutdown();
        verify(CommitCompletedConsumerMock.get("hap2")).shutdown();
        verify(CommitFailedConsumerMock.get("hap1")).shutdown();
        verify(CommitFailedConsumerMock.get("hap2")).shutdown();
    }

    @Test
    public void should_shutdown_all_consumers_when_asked() {
        actionsObservable.onNext(ManagedHaproxy.HaproxyAction.register("hap1"));
        actionsObservable.onNext(ManagedHaproxy.HaproxyAction.register("hap2"));

        incomingEvents.shutdownConsumers();

        verify(CommitCompletedConsumerMock.get("hap1")).shutdown();
        verify(CommitCompletedConsumerMock.get("hap2")).shutdown();
        verify(CommitFailedConsumerMock.get("hap1")).shutdown();
        verify(CommitFailedConsumerMock.get("hap2")).shutdown();
        verify(RegisterServerConsumerMock.get("single")).shutdown();
    }

    @Test
    public void should_broadcast_register_server_events() {
        List<String> observedEvents = new ArrayList<>();

        incomingEvents.registerServerEventObservable().subscribe(event -> {
            observedEvents.add(event.getCorrelationId());
        });

        RegisterServerConsumerMock.sendEvent("single", "1-1");
        RegisterServerConsumerMock.sendEvent("single", "1-2");
        RegisterServerConsumerMock.sendEvent("single", "1-3");

        List<String> expected = Arrays.asList("1-1", "1-2", "1-3");
        assertThat(observedEvents, is(expected));
    }

    @Test
    public void should_respect_commit_completed_consumers_backpressure() {
        TestSubscriber subscriber = new TestSubscriber(1);
        incomingEvents.commitSuccessEventObservale().subscribe(subscriber);

        actionsObservable.onNext(ManagedHaproxy.HaproxyAction.register("hap1"));

        CommitCompletedConsumerMock.sendEvent("hap1", "1-1");
        CommitCompletedConsumerMock.sendEvent("hap1", "1-2");

        subscriber.assertNoErrors();
    }

    @Test
    public void should_respect_commit_failed_consumers_backpressure() {
        TestSubscriber subscriber = new TestSubscriber(1);
        incomingEvents.commitFailureEventObservale().subscribe(subscriber);

        actionsObservable.onNext(ManagedHaproxy.HaproxyAction.register("hap1"));

        CommitFailedConsumerMock.sendEvent("hap1", "1-1");
        CommitFailedConsumerMock.sendEvent("hap1", "1-2");

        subscriber.assertNoErrors();
    }

    @Test
    public void should_respect_register_server_consumers_backpressure() {
        TestSubscriber subscriber = new TestSubscriber(1);
        incomingEvents.registerServerEventObservable().subscribe(subscriber);

        RegisterServerConsumerMock.sendEvent("single", "1-1");
        RegisterServerConsumerMock.sendEvent("single", "1-2");

        subscriber.assertNoErrors();
    }

    static class CommitCompletedConsumerMock {
        static Map<String, CommitCompletedConsumerMock> mocks = new HashMap<>();

        CommitCompletedConsumer            mock = mock(CommitCompletedConsumer.class);
        PublishSubject<CommitSuccessEvent> s    = PublishSubject.create();

        CommitCompletedConsumerMock(String id) {
            when(mock.observable()).thenReturn(s.onBackpressureBuffer());
        }

        void sendEvent(String correlationId) {
            s.onNext(new CommitSuccessEvent(correlationId, () -> "CommitCompletedConsumerMock"));
        }

        static CommitCompletedConsumer create(String id) {
            CommitCompletedConsumerMock mockWrapper = new CommitCompletedConsumerMock(id);
            mocks.put(id, mockWrapper);
            return mockWrapper.mock;
        }

        static void sendEvent(String id, String correlationId) {
            mocks.get(id).sendEvent(correlationId);
        }

        public static CommitCompletedConsumer get(String id) {
            return mocks.get(id).mock;
        }

        static void clear() {
            mocks.clear();
        }
    }

    static class CommitFailedConsumerMock {
        static Map<String, CommitFailedConsumerMock> mocks = new HashMap<>();

        CommitFailedConsumer               mock = mock(CommitFailedConsumer.class);
        PublishSubject<CommitFailureEvent> s    = PublishSubject.create();

        CommitFailedConsumerMock(String id) {
            when(mock.observable()).thenReturn(s.onBackpressureBuffer());
        }

        void sendEvent(String correlationId) {
            s.onNext(new CommitFailureEvent(correlationId, () -> "CommitFailedConsumerMock"));
        }

        static CommitFailedConsumer create(String id) {
            CommitFailedConsumerMock mockWrapper = new CommitFailedConsumerMock(id);
            mocks.put(id, mockWrapper);
            return mockWrapper.mock;
        }

        static void sendEvent(String id, String correlationId) {
            mocks.get(id).sendEvent(correlationId);
        }

        public static CommitFailedConsumer get(String id) {
            return mocks.get(id).mock;
        }

        static void clear() {
            mocks.clear();
        }

    }

    static class RegisterServerConsumerMock {
        static Map<String, RegisterServerConsumerMock> mocks = new HashMap<>();

        RegisterServerConsumer              mock = mock(RegisterServerConsumer.class);
        PublishSubject<RegisterServerEvent> s    = PublishSubject.create();

        RegisterServerConsumerMock(String id) {
            when(mock.observable()).thenReturn(s.onBackpressureBuffer());
        }

        void sendEvent(String correlationId) {
            s.onNext(new RegisterServerEvent(correlationId, () -> "RegisterServerConsumerMock", "backend", new HashSet<>()));
        }

        static RegisterServerConsumer create(String id) {
            RegisterServerConsumerMock mockWrapper = new RegisterServerConsumerMock(id);
            mocks.put(id, mockWrapper);
            return mockWrapper.mock;
        }

        static void sendEvent(String id, String correlationId) {
            mocks.get(id).sendEvent(correlationId);
        }

        public static RegisterServerConsumer get(String id) {
            return mocks.get(id).mock;
        }

        static void clear() {
            mocks.clear();
        }
    }

}
