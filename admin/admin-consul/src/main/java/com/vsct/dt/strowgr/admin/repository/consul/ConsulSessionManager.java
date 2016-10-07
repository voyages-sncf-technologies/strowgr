package com.vsct.dt.strowgr.admin.repository.consul;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import java.util.concurrent.TimeUnit;

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
public class ConsulSessionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulSessionManager.class);

    private final ConsulConnection consulConnection;
    private final Observable<Long> timer;
    private final int              ttl;
    private final SessionRenewer   sessionRenewer;

    private volatile ConsulRepository.Session currentSession;

    public ConsulSessionManager(ConsulConnection consulConnection, int ttl) {
        this(consulConnection, ttl, Schedulers.newThread());
    }

    ConsulSessionManager(ConsulConnection consulConnection, int ttl, Scheduler scheduler) {
        this(consulConnection, ttl, Observable.interval((long) Math.floor(ttl / 2), TimeUnit.SECONDS, scheduler));
    }

    private ConsulSessionManager(ConsulConnection consulConnection, int ttl, Observable<Long> timer) {
        this.consulConnection = consulConnection;
        this.ttl = ttl;
        this.timer = timer;
        this.sessionRenewer = this.new SessionRenewer();
    }

    public void start() {
        LOGGER.debug("create new currentSession");
        createSession().subscribe(session -> timer.subscribe(sessionRenewer));
    }

    public ConsulRepository.Session getSession() {
        if (currentSession == null) {
            throw new IllegalStateException("Tried to get session but it has not been set yet. ConsulSessionManager needs to be started first.");
        }
        return currentSession;
    }

    public void stop() {
        LOGGER.debug("destroy current session");

        sessionRenewer.unsubscribe();

        consulConnection.destroySession(currentSession.getID()).subscribe(
                session -> {
                    currentSession = null;
                    LOGGER.debug("current session has been destroyed");
                },
                error -> LOGGER.info("session could not be destroyed. it will be destroyed via TTL ({}s)", ttl)
        );
    }

    private Observable<ConsulRepository.Session> createSession() {
        return consulConnection
                .createSession(ttl, ConsulRepository.SESSION_BEHAVIOR.DELETE)
                .doOnNext(session -> {
                    currentSession = session;
                    LOGGER.debug("currentSession created with ttl {} and behavior {}", ttl, ConsulRepository.SESSION_BEHAVIOR.DELETE);
                })
                .doOnError(
                        error -> LOGGER.error("session could not be created. Application cannot work properlly, this needs further investigation. Cause {]", error.getCause())
                );
    }

    private class SessionRenewer extends Subscriber<Long> {

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            LOGGER.error("Unexpected error happened in SessionRenewer. Cause {}", e.getCause());
        }

        @Override
        public void onNext(Long l) {
            if (currentSession != null) {
                consulConnection.renewSession(currentSession.getID()).subscribe(
                        session -> LOGGER.debug("session {} has been renewed", session.getID()),
                        error -> {
                            LOGGER.info("session could not be renewed. needs to create a new session");
                            createSession().subscribe();
                        }
                );
            }
        }
    }
}
