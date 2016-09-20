package com.vsct.dt.strowgr.admin.repository.consul;

import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.*;

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
public class ConsulSessionManagerTest {

    ConsulSessionManager consulSessionManager;
    ConsulConnection consulConnection;
    TestScheduler scheduler;

    int TTL = 10;

    @Before
    public void setUp(){
        scheduler = new TestScheduler();
        consulConnection = mock(ConsulConnection.class);
        consulSessionManager = new ConsulSessionManager(consulConnection, TTL, scheduler);
    }

    @Test
    public void should_create_consul_session_with_ttl_when_started(){
        consulSessionManager.start();

        verify(consulConnection).createSession(TTL, ConsulRepository.SESSION_BEHAVIOR.DELETE);
    }

    @Test(expected = IllegalStateException.class)
    public void should_throw_an_exception_if_session_is_not_present(){
        consulSessionManager.getSession();
    }

    @Test
    public void should_renew_session_at_half_lifetime(){
        when(consulConnection.createSession(TTL, ConsulRepository.SESSION_BEHAVIOR.DELETE)).thenReturn(new ConsulRepository.Session("id"));

        consulSessionManager.start();

        scheduler.advanceTimeBy(TTL+2, TimeUnit.SECONDS);

        verify(consulConnection, times(2)).renewSession("id");
    }

    @Test
    public void should_return_session_when_asked(){
        when(consulConnection.createSession(TTL, ConsulRepository.SESSION_BEHAVIOR.DELETE)).thenReturn(new ConsulRepository.Session("id"));

        consulSessionManager.start();

        ConsulRepository.Session session = consulSessionManager.getSession();

        assertThat(session.getID(), is("id"));
    }

    @Test
    public void should_destroy_session_when_shutdown(){
        when(consulConnection.createSession(TTL, ConsulRepository.SESSION_BEHAVIOR.DELETE)).thenReturn(new ConsulRepository.Session("id"));
        consulSessionManager.start();

        consulSessionManager.stop();

        verify(consulConnection).destroySession("id");

        try{
            consulSessionManager.getSession();
        } catch (Exception e){
            return;
        }
        fail();
    }

}
