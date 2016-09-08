/*
 *  Copyright (C) 2016 VSCT
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.vsct.dt.strowgr.admin.gui.managed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.vsct.dt.strowgr.admin.core.event.in.EntryPointEvent;
import com.vsct.dt.strowgr.admin.nsq.consumer.CommitCompletedConsumer;
import com.vsct.dt.strowgr.admin.nsq.consumer.CommitFailedConsumer;
import com.vsct.dt.strowgr.admin.nsq.consumer.ObservableNSQConsumer;
import com.vsct.dt.strowgr.admin.repository.consul.ConsulRepository;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.AsyncEmitter;
import rx.Observable;
import rx.observables.ConnectableObservable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * NSQ client manager by Dropwizard.
 */
public class ConsumableTopics implements Managed {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumableTopics.class);

    /* A single thread will perform the IO action */
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private final NSQLookup lookup;
    private final ObjectMapper objectMapper;

    /* Reference to emitter to properly stop the manager and advice subscribers */
    private volatile AsyncEmitter emitter;

    /* This map is always called from a single thread (either the scheduledServiceExecutor or the shutdown thread of dropwizard, no need to synchronize */
    private final HashMap<String, ArrayList<ObservableNSQConsumer>> consumers = new HashMap<>();
    private final ConnectableObservable<? extends EntryPointEvent> observable;

    public ConsumableTopics(ConsulRepository repository, NSQLookup lookup, ObjectMapper objectMapper, long periodSecond) {
        this.lookup = lookup;
        this.objectMapper = objectMapper;
        this.observable = Observable.<ObservableNSQConsumer>fromEmitter(emitter -> {
            /* Keep reference to emitter for proper shutdown */
            this.emitter = emitter;
            /* Do the business */
            scheduledExecutorService.scheduleAtFixedRate(() -> refreshHaproxyTopicsConsumers(repository, emitter), 0, periodSecond, TimeUnit.SECONDS);
            /* Stop when cancelled */
            emitter.setCancellation(this::stop);
        }, AsyncEmitter.BackpressureMode.BUFFER).flatMap(consumer -> consumer.observe()).publish();//We dont want to loose any subscription (they wont happen a lot !)
    }

    public ConnectableObservable<? extends EntryPointEvent> observe() {
        return observable;
    }

    private void refreshHaproxyTopicsConsumers(ConsulRepository repository, AsyncEmitter<ObservableNSQConsumer> emitter) {
        Set<String> ids = repository.getHaproxyIds().orElseGet(HashSet::new);

        //Find all removed haproxies
        Set<String> removedHaproxies = consumers.keySet()
                .stream()
                .filter(k -> !ids.contains(k))
                .collect(Collectors.toSet());

        // Stop consumers and remove them from store map
        // TODO Could be more RXJava compliant to do the shutdown during unsubscription/cancellation of the subscriber
        removedHaproxies.forEach(this::shutdownAndRemoveConsumers);

        // Find all new haproxies
        Set<String> newHaproxies = ids
                .stream()
                .filter(k -> !consumers.containsKey(k))
                .collect(Collectors.toSet());

        // Register those haproxies in store map and emit them
        newHaproxies.forEach(id -> storeAndEmitConsumers(id, emitter));
    }

    private void shutdownAndRemoveConsumers(String id) {
        consumers.get(id).forEach(ObservableNSQConsumer::shutdown);
        consumers.remove(id);
    }

    private void storeAndEmitConsumers(String id, AsyncEmitter<ObservableNSQConsumer> emitter) {
        CommitCompletedConsumer commitCompletedConsumer = new CommitCompletedConsumer(lookup, id, objectMapper);
        CommitFailedConsumer commitFailedConsumer = new CommitFailedConsumer(lookup, id, objectMapper);

        ArrayList<ObservableNSQConsumer> list = new ArrayList<>();
        list.add(commitCompletedConsumer);
        list.add(commitFailedConsumer);
        consumers.put(id, list);

        emitter.onNext(commitCompletedConsumer);
        emitter.onNext(commitFailedConsumer);
    }

    @Override
    public void start() throws Exception {
        observable.connect();
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("stop all nsqconsumers");
        scheduledExecutorService.shutdown();
        if (emitter != null) {
            emitter.onCompleted();
        }
    }
}
