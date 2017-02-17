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

import com.vsct.dt.strowgr.admin.gui.observable.HAProxyPublisher.HAProxyAction;
import com.vsct.dt.strowgr.admin.nsq.consumer.FlowableNSQConsumer;
import io.reactivex.Flowable;
import io.reactivex.subscribers.TestSubscriber;
import org.junit.Test;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.Mockito.*;

public class HAProxySubscriberTest {

    @SuppressWarnings("unchecked")
    private final Function<String, FlowableNSQConsumer<Integer>> nsqConsumerBuilder = mock(Function.class);

    private TestSubscriber<Integer> subscriber = new TestSubscriber<>();

    private final HAProxySubscriber<Integer> actionToCommitEventConsumer = new HAProxySubscriber<>(nsqConsumerBuilder, subscriber);

    @Test
    public void should_create_nsq_consumer_on_ha_proxy_register_action_and_forward_nsq_consumers_messages_to_subscriber() throws Exception {
        // given
        @SuppressWarnings("unchecked")
        FlowableNSQConsumer<Integer> nsqConsumer1 = mock(FlowableNSQConsumer.class);
        when(nsqConsumer1.flowable()).thenReturn(Flowable.range(0, 10));
        when(nsqConsumerBuilder.apply("haProxy1")).thenReturn(nsqConsumer1);
        @SuppressWarnings("unchecked")
        FlowableNSQConsumer<Integer> nsqConsumer2 = mock(FlowableNSQConsumer.class);
        when(nsqConsumer2.flowable()).thenReturn(Flowable.range(10, 10));
        when(nsqConsumerBuilder.apply("haProxy2")).thenReturn(nsqConsumer2);

        // when
        actionToCommitEventConsumer.onNext(HAProxyAction.register("haProxy1"));
        actionToCommitEventConsumer.onNext(HAProxyAction.register("haProxy2"));

        // then
        subscriber.hasSubscription();
        subscriber.assertSubscribed();
        subscriber.assertValueSet(IntStream.range(0, 20).mapToObj(Integer::valueOf).collect(Collectors.toSet()));
    }

    @Test
    public void should_shutdown_nsq_consumer_on_unregister_action() throws Exception {
        // given
        @SuppressWarnings("unchecked")
        FlowableNSQConsumer<Integer> nsqConsumer = mock(FlowableNSQConsumer.class);
        when(nsqConsumer.flowable()).thenReturn(Flowable.range(0, 10));
        when(nsqConsumerBuilder.apply("haProxy1")).thenReturn(nsqConsumer);

        // when
        actionToCommitEventConsumer.onNext(HAProxyAction.register("haProxy1"));

        // then
        subscriber.hasSubscription();
        subscriber.assertSubscribed();
        subscriber.assertValueSet(IntStream.range(0, 20).mapToObj(Integer::valueOf).collect(Collectors.toSet()));

        // when
        actionToCommitEventConsumer.onNext(HAProxyAction.unregister("haProxy1"));

        // then
        verify(nsqConsumer).shutdown();
    }

    @Test
    public void should_shutdown_all_nsq_consumers_when_stopped() throws Exception {
        // given
        @SuppressWarnings("unchecked")
        FlowableNSQConsumer<Integer> nsqConsumer1 = mock(FlowableNSQConsumer.class);
        when(nsqConsumer1.flowable()).thenReturn(Flowable.range(0, 10));
        when(nsqConsumerBuilder.apply("haProxy1")).thenReturn(nsqConsumer1);
        @SuppressWarnings("unchecked")
        FlowableNSQConsumer<Integer> nsqConsumer2 = mock(FlowableNSQConsumer.class);
        when(nsqConsumer2.flowable()).thenReturn(Flowable.range(10, 10));
        when(nsqConsumerBuilder.apply("haProxy2")).thenReturn(nsqConsumer2);

        // when
        actionToCommitEventConsumer.onNext(HAProxyAction.register("haProxy1"));
        actionToCommitEventConsumer.onNext(HAProxyAction.register("haProxy2"));

        // then
        subscriber.hasSubscription();
        subscriber.assertSubscribed();
        subscriber.assertValueSet(IntStream.range(0, 20).mapToObj(Integer::valueOf).collect(Collectors.toSet()));

        // when
        actionToCommitEventConsumer.stop();

        // then
        verify(nsqConsumer1).shutdown();
        verify(nsqConsumer2).shutdown();
    }
}