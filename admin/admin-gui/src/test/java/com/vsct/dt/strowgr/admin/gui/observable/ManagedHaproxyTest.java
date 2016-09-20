package com.vsct.dt.strowgr.admin.gui.observable;

import com.vsct.dt.strowgr.admin.repository.consul.ConsulRepository;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.TestScheduler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

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
public class ManagedHaproxyTest {

    ConsulRepository repository;

    @Before
    public void setUp(){
        repository = mock(ConsulRepository.class);
    }

    @Test
    public void should_observe_registration_actions(){
        HashSet<String> managedHap = new HashSet<>();
        managedHap.add("hap1");
        managedHap.add("hap2");
        when(repository.getHaproxyIds()).thenReturn(Optional.of(managedHap));

        TestScheduler scheduler = new TestScheduler();
        ManagedHaproxy managedHaproxy = ManagedHaproxy.create(repository, 1, scheduler);
        Observable<ManagedHaproxy.HaproxyAction> registrationActionsObservable = managedHaproxy.registrationActionsObservable();

        /* Use list to also count elements */
        List<String> registeredHap = new ArrayList<>();
        Subscription s = registrationActionsObservable.subscribe(a -> {
            if(a.isRegistration()){
                registeredHap.add(a.getId());
            }
        });

        managedHaproxy.startLookup();

        scheduler.advanceTimeBy(10, TimeUnit.SECONDS);

        managedHap.add("hap3");

        scheduler.advanceTimeBy(10, TimeUnit.SECONDS);

        s.unsubscribe();

        assertThat(registeredHap.size(), is(3));
        assertThat(registeredHap, is(new ArrayList(managedHap)));
    }

    @Test
    public void should_observe_unregistration_actions(){
        HashSet<String> managedHap = new HashSet<>();
        managedHap.add("hap1");
        managedHap.add("hap2");
        managedHap.add("hap3");
        managedHap.add("hap4");
        when(repository.getHaproxyIds()).thenReturn(Optional.of(managedHap));

        TestScheduler scheduler = new TestScheduler();
        ManagedHaproxy managedHaproxy = ManagedHaproxy.create(repository, 1, scheduler);
        Observable<ManagedHaproxy.HaproxyAction> registrationActionsObservable = managedHaproxy.registrationActionsObservable();

        /* Use list to also count elements */
        List<String> unregisteredHap = new ArrayList<>();
        Subscription s = registrationActionsObservable.subscribe(a -> {
            if(!a.isRegistration()){
                unregisteredHap.add(a.getId());
            }
        });

        managedHaproxy.startLookup();

        scheduler.advanceTimeBy(10, TimeUnit.SECONDS);

        managedHap.remove("hap3");

        scheduler.advanceTimeBy(10, TimeUnit.SECONDS);

        managedHap.remove("hap4");

        scheduler.advanceTimeBy(10, TimeUnit.SECONDS);

        s.unsubscribe();

        assertThat(unregisteredHap.size(), is(2));
        List<String> expected = new ArrayList<>();
        expected.add("hap3");
        expected.add("hap4");
        assertThat(unregisteredHap, is(expected));
    }

    @Test
    public void should_stop_observable(){
        TestScheduler scheduler = new TestScheduler();
        ManagedHaproxy managedHaproxy = ManagedHaproxy.create(repository, 1, scheduler);
        Observable<ManagedHaproxy.HaproxyAction> registrationActionsObservable = managedHaproxy.registrationActionsObservable();

        Subscription s = registrationActionsObservable.subscribe();

        managedHaproxy.startLookup();
        managedHaproxy.stopLookup();

        assertThat(s.isUnsubscribed(), is(true));
    }

}
