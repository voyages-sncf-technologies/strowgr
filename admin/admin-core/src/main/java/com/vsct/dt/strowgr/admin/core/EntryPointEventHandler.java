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
package com.vsct.dt.strowgr.admin.core;

import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPointFrontend;
import com.vsct.dt.strowgr.admin.core.configuration.IncomingEntryPointBackendServer;
import com.vsct.dt.strowgr.admin.core.event.in.*;
import com.vsct.dt.strowgr.admin.core.event.out.CommitRequestedEvent;
import com.vsct.dt.strowgr.admin.core.repository.HaproxyRepository;
import com.vsct.dt.strowgr.admin.core.repository.PortRepository;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class EntryPointEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryPointEventHandler.class);

    private final EntryPointStateManager stateManager;
    private final HaproxyRepository haproxyRepository;
    private final TemplateGenerator templateGenerator;
    private final TemplateLocator templateLocator;
    private final PortRepository portRepository;

    private final Subscriber<CommitRequestedEvent> commitRequestedSubscriber;

    public EntryPointEventHandler(EntryPointStateManager stateManager, PortRepository portRepository,
                                  HaproxyRepository haproxyRepository, TemplateLocator templateLocator,
                                  TemplateGenerator templateGenerator,
                                  Subscriber<CommitRequestedEvent> commitRequestedSubscriber) {
        this.stateManager = stateManager;
        this.portRepository = portRepository;
        this.haproxyRepository = haproxyRepository;
        this.templateLocator = templateLocator;
        this.templateGenerator = templateGenerator;
        this.commitRequestedSubscriber = commitRequestedSubscriber;
    }

    public void handle(RegisterServerEvent event) {
        LOGGER.info("handles {}", event);
        EntryPointKey key = event.getKey();
        try {

            if (this.stateManager.lock(key)) {
                Optional<EntryPoint> existingConfiguration = Optional.ofNullable(
                        stateManager.getPendingConfiguration(key)
                                .orElseGet(() -> stateManager.getCommittingConfiguration(key)
                                        .orElseGet(() -> stateManager.getCurrentConfiguration(key)
                                                .orElseGet(() -> {
                                                    LOGGER.warn("can't find an entrypoint for key {} from register server event {}", key, event);
                                                    return null;
                                                })))
                );

                existingConfiguration.map(c -> c.registerServers(event.getBackend(), event.getServers())).ifPresent(c -> {
                    Optional<EntryPoint> preparedConfiguration = stateManager.prepare(key, c);

                    if (preparedConfiguration.isPresent()) {
                        LOGGER.info("new servers registered for EntryPoint {}", event.getKey().getID());
                        if (LOGGER.isDebugEnabled()) {
                            for (IncomingEntryPointBackendServer server : event.getServers()) {
                                LOGGER.debug("- registered server {}", server);
                            }
                        }
                    } else {
                        LOGGER.warn("can't prepare configuration on key {}", key);
                    }
                });
            }

        } catch (Exception e) {
            LOGGER.error("Following error occurred while registering server {} for entry point {}", event, key, e);
        } finally {
            this.stateManager.release(key);
        }
    }

    public void handle(TryCommitCurrentConfigurationEvent event) throws IncompleteConfigurationException {
        LOGGER.debug("handles {}", event);
        EntryPointKey entryPointKey = event.getKey();
        try {
            if (this.stateManager.lock(entryPointKey)) {
                Optional<EntryPoint> entryPoint = stateManager.tryCommitCurrent(event.getCorrelationId(), entryPointKey);
                if (entryPoint.isPresent()) {
                    EntryPoint configuration = entryPoint.get();
                    if (!haproxyRepository.isAutoreload(configuration.getHaproxy()) || !stateManager.isAutoreloaded(entryPointKey)) {
                        stateManager.cancelCommit(entryPointKey);
                        LOGGER.debug("skip tryCommitCurrent for event {} because haproxy {} or entrypoint {} is not in autoreload mode", event, configuration.getHaproxy(), entryPointKey);
                    } else {
                        CommitRequestedEvent commitRequestedEvent = getCommitRequestedEvent(event.getCorrelationId(), entryPointKey, configuration);
                        LOGGER.trace("from handle -> post to event bus event {}", commitRequestedEvent);
                        commitRequestedSubscriber.onNext(commitRequestedEvent);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Following error occurred while committing current configuration {} for entry point {}", event, entryPointKey, e);
        } finally {
            this.stateManager.release(entryPointKey);
        }
    }

    public void handle(TryCommitPendingConfigurationEvent event) throws IncompleteConfigurationException {
        LOGGER.debug("handles {}", event);
        EntryPointKey entryPointKey = event.getKey();
        try {
            if (this.stateManager.lock(entryPointKey)) {
                Optional<EntryPoint> entryPoint = stateManager.tryCommitPending(event.getCorrelationId(), entryPointKey);
                if (entryPoint.isPresent()) {
                    EntryPoint configuration = entryPoint.get();
                    if (!haproxyRepository.isAutoreload(configuration.getHaproxy()) || !stateManager.isAutoreloaded(entryPointKey)) {
                        stateManager.cancelCommit(entryPointKey);
                        stateManager.prepare(entryPointKey, configuration);
                        LOGGER.debug("skip tryCommitPending for event {} because haproxy {}  or entrypoint {} is not in autoreload mode", event, configuration.getHaproxy(), entryPointKey);
                    } else {
                        CommitRequestedEvent commitRequestedEvent = getCommitRequestedEvent(event.getCorrelationId(), entryPointKey, configuration);
                        LOGGER.trace("from handle -> post to event bus event {}", commitRequestedEvent);
                        commitRequestedSubscriber.onNext(commitRequestedEvent);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Following error occurred while committing pending configuration {} for entry point {}", event, entryPointKey, e);
        } finally {
            this.stateManager.release(entryPointKey);
        }
    }

    private CommitRequestedEvent getCommitRequestedEvent(String correlationId, EntryPointKey entryPointKey, EntryPoint configuration) throws IncompleteConfigurationException {

        String template = templateLocator.readTemplate(configuration).orElseThrow(() -> new RuntimeException("Could not find any template for configuration " + entryPointKey));

        Map<String, Integer> portsMapping = new HashMap<>();

        int syslogPort = portRepository.getPort(entryPointKey, configuration.syslogPortId()).orElseGet(() -> portRepository.newPort(entryPointKey, configuration.syslogPortId()));
        portsMapping.put(configuration.syslogPortId(), syslogPort);

        for (EntryPointFrontend frontend : configuration.getFrontends()) {
            int frontendPort = portRepository.getPort(entryPointKey, frontend.portId()).orElseGet(() -> portRepository.newPort(entryPointKey, frontend.portId()));
            portsMapping.put(frontend.portId(), frontendPort);
        }

        String conf = templateGenerator.generate(template, configuration, portsMapping);
        String syslogConf = templateGenerator.generateSyslogFragment(configuration, portsMapping);
        String bind = haproxyRepository.getHaproxyProperty(configuration.getHaproxy(), "binding/" + configuration.getBindingId()).orElseThrow(() -> new IllegalStateException("Could not find binding " + configuration.getBindingId() + " for haproxy " + configuration.getHaproxy()));
        return new CommitRequestedEvent(correlationId, entryPointKey, configuration, conf, syslogConf, bind);
    }

    public void handle(CommitSuccessEvent event) {
        LOGGER.debug("handles {}", event);
        EntryPointKey key = event.getKey();
        try {
            if (this.stateManager.lock(key)) {
                Optional<String> optionalCorrelationId = stateManager.getCommitCorrelationId(key);
                if (optionalCorrelationId.isPresent() && optionalCorrelationId.get().equals(event.getCorrelationId())) {
                    Optional<EntryPoint> currentConfiguration = stateManager.commit(key);
                    if (currentConfiguration.isPresent()) {
                        LOGGER.debug("Configuration for EntryPoint {} has been committed", event.getKey().getID());
                    } else {
                        LOGGER.error("Configuration for EntryPoint {} could not be committed", event.getKey().getID());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Following error occurred while publishing commit success {} for entry point {}", event, key, e);
        } finally {
            this.stateManager.release(key);
        }
    }

    public void handle(CommitFailureEvent event) {
        LOGGER.debug("handles CommitFailureEvent");
        EntryPointKey key = event.getKey();
        try {
            if (this.stateManager.lock(key)) {
                Optional<String> commitCorrelationId = stateManager.getCommitCorrelationId(key);
                if (commitCorrelationId.isPresent() && commitCorrelationId.get().equals(event.getCorrelationId())) {
                    LOGGER.info("Configuration for EntryPoint {} failed. Commit is canceled.", key);
                    stateManager.cancelCommit(key);
                } else {
                    LOGGER.info("Received a failed event but either there is no committing configuration or the correlation id does not match.");
                }
            }
        } catch (Exception e) {
            LOGGER.error("Following error occurred while publishing commit failure {} for entry point {}", event, key, e);
        } finally {
            this.stateManager.release(key);
        }
    }

}
