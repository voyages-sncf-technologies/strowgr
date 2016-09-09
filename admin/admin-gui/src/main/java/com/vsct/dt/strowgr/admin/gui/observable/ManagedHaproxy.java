package com.vsct.dt.strowgr.admin.gui.observable;

import com.vsct.dt.strowgr.admin.repository.consul.ConsulRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
public class ManagedHaproxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedHaproxy.class);

    private final ConsulRepository repository;
    private final Set<String> registered = new HashSet<>();

    //The connectable observable offered to subscribers
    private final ConnectableObservable<HaproxyAction> registrationActionsObservable;
    //Subject to stop the observable
    private final PublishSubject stop = PublishSubject.create();

    private ManagedHaproxy(ConsulRepository repository, long intervalSecond, Scheduler scheduler) {
        this.repository = repository;

        this.registrationActionsObservable = Observable.interval(intervalSecond, TimeUnit.SECONDS, scheduler)
                .map(n -> lookupHaproxy())
                .flatMap(Observable::from)
                .takeUntil(stop)
                .publish(); //We want to control the start of the observable
    }

    //Return observable so that observable cannot be started withotu calling start method of ManagedHaproxy
    public Observable<HaproxyAction> registrationActionsObservable() {
        return registrationActionsObservable;
    }

    public static ManagedHaproxy create(ConsulRepository repository, long intervalSecond) {
        return new ManagedHaproxy(repository, intervalSecond, Schedulers.newThread());
    }

    //package protected method for test purposes
    static ManagedHaproxy create(ConsulRepository repository, long intervalSecond, Scheduler scheduler) {
        return new ManagedHaproxy(repository, intervalSecond, scheduler);
    }

    private List<HaproxyAction> lookupHaproxy() {

        LOGGER.info("lookup registration action on haproxy");

        List<HaproxyAction> actions = new ArrayList<>();

        Set<String> ids = repository.getHaproxyIds().orElseGet(HashSet::new);

        //Find all removed haproxies
        Set<String> removedHaproxies = registered
                .stream()
                .filter(k -> !ids.contains(k))
                .peek(id -> actions.add(HaproxyAction.unregister(id)))
                .collect(Collectors.toSet());

        registered.removeAll(removedHaproxies);

        // Find all new haproxies
        Set<String> addedHaproxies = ids.stream()
                .filter(k -> !registered.contains(k))
                .peek(id -> actions.add(HaproxyAction.register(id)))
                .collect(Collectors.toSet());

        registered.addAll(addedHaproxies);

        if (LOGGER.isInfoEnabled()) {
            if (actions.size() > 0) {
                actions.forEach(a -> LOGGER.info("registration action on haproxy id={} -> register={}", a.id, a.isRegistration));
            }
            else {
                LOGGER.info("no registration action to perform");
            }
        }

        return actions;
    }

    public void startLookup() {
        this.registrationActionsObservable.connect();
    }

    public void stopLookup() {
        stop.onNext(null);
    }

    public static class HaproxyAction {
        private final String  id;
        private final boolean isRegistration;

        private HaproxyAction(String id, boolean register) {
            this.id = id;
            this.isRegistration = register;
        }

        public String getId() {
            return id;
        }

        public boolean isRegistration() {
            return isRegistration;
        }

        public static HaproxyAction register(String id) {
            return new HaproxyAction(id, true);
        }

        public static HaproxyAction unregister(String id) {
            return new HaproxyAction(id, false);
        }
    }

}