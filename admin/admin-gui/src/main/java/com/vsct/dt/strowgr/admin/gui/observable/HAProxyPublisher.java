package com.vsct.dt.strowgr.admin.gui.observable;

import com.vsct.dt.strowgr.admin.repository.consul.ConsulRepository;
import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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
public class HAProxyPublisher implements Function<Long, Flowable<HAProxyPublisher.HAProxyAction>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HAProxyPublisher.class);

    private final ConsulRepository repository;

    private final Set<String> registered = new HashSet<>();

    public HAProxyPublisher(ConsulRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public Flowable<HAProxyAction> apply(Long n) {

        LOGGER.debug("Lookup registration actions for HAProxy instances");

        try {

            List<HAProxyAction> actions = new ArrayList<>();

            final Set<String> currentIdSet = repository.getHaproxyIds();

            // Find all removed HA Proxy instances
            Set<String> removedHAProxies = registered.stream()
                    .filter(k -> !currentIdSet.contains(k))
                    .peek(id -> actions.add(HAProxyAction.unregister(id)))
                    .collect(Collectors.toSet());
            registered.removeAll(removedHAProxies);

            // Find all new HA Proxy instances
            Set<String> addedHAProxies = currentIdSet.stream()
                    .filter(k -> !registered.contains(k))
                    .peek(id -> actions.add(HAProxyAction.register(id)))
                    .collect(Collectors.toSet());

            registered.addAll(addedHAProxies);

            if (LOGGER.isInfoEnabled()) {
                if (actions.size() > 0) {
                    actions.forEach(a -> LOGGER.info("registration action on haproxy id={} -> register={}", a.id, a.isRegistration));
                } else {
                    LOGGER.debug("no registration action to perform");
                }
            }

            return Flowable.fromIterable(actions);

        } catch (Exception e) {
            LOGGER.error("Unable to lookup HAProxy IDs.", e);
            return Flowable.empty();
        }

    }

    public static class HAProxyAction {

        private final String id;

        private final boolean isRegistration;

        private HAProxyAction(String id, boolean register) {
            this.id = id;
            this.isRegistration = register;
        }

        public String getId() {
            return id;
        }

        boolean isRegistration() {
            return isRegistration;
        }

        static HAProxyAction register(String id) {
            LOGGER.info("register haproxy {}", id);
            return new HAProxyAction(id, true);
        }

        static HAProxyAction unregister(String id) {
            LOGGER.info("unregister haproxy {}", id);
            return new HAProxyAction(id, false);
        }
    }

}