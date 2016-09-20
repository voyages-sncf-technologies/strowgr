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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPointFrontend;
import com.vsct.dt.strowgr.admin.core.configuration.IncomingEntryPointBackendServer;
import com.vsct.dt.strowgr.admin.core.event.in.*;
import com.vsct.dt.strowgr.admin.core.event.out.*;
import com.vsct.dt.strowgr.admin.core.repository.EntryPointRepository;
import com.vsct.dt.strowgr.admin.core.repository.HaproxyRepository;
import com.vsct.dt.strowgr.admin.core.repository.PortRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class EntryPointEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryPointEventHandler.class);

    private final EntryPointStateManager stateManager;
    private HaproxyRepository haproxyRepository;
    private final EventBus outputBus;
    private final TemplateGenerator templateGenerator;
    private final TemplateLocator templateLocator;
    private final PortRepository portRepository;

    EntryPointEventHandler(EntryPointStateManager stateManager, PortRepository portRepository, HaproxyRepository haproxyRepository, TemplateLocator templateLocator, TemplateGenerator templateGenerator, EventBus outputBus) {
        this.stateManager = stateManager;
        this.outputBus = outputBus;
        this.portRepository = portRepository;
        this.haproxyRepository = haproxyRepository;
        this.templateLocator = templateLocator;
        this.templateGenerator = templateGenerator;
    }

    public static EntryPointEventHandlerBuilder backedBy(EntryPointRepository entryPointRepository, HaproxyRepository haproxyRepository) {
        return new EntryPointEventHandlerBuilder(entryPointRepository, haproxyRepository);
    }

    @Subscribe
    public void handle(AddEntryPointEvent event) {
        EntryPointKey entryPointKey = event.getKey();
        try {
            EntryPoint entryPoint = event.getConfiguration().orElseThrow(() -> new IllegalStateException("can't retrieve configuration of event " + event));

            // force disabled/enabled of entrypoint even if lock will fail
            if (this.haproxyRepository.getHaproxyProperty(entryPoint.getHaproxy(), "platform").orElse("").equals("production")) {
                this.stateManager.setDisabled(entryPointKey, true);
            }

            this.stateManager.lock(entryPointKey);
            if (!stateManager.getCommittingConfiguration(entryPointKey).isPresent() && !stateManager.getCurrentConfiguration(entryPointKey).isPresent()) {
                Optional<EntryPoint> preparedConfiguration = stateManager.prepare(entryPointKey, entryPoint);

                if (preparedConfiguration.isPresent()) {
                    EntryPointAddedEvent entryPointAddedEvent = new EntryPointAddedEvent(event.getCorrelationId(), entryPointKey, preparedConfiguration.get());
                    LOGGER.info("from handle AddEntryPointEvent new EntryPoint {} added -> {}", entryPointKey.getID(), entryPointAddedEvent);
                    outputBus.post(entryPointAddedEvent);
                }
            }
        } finally {
            this.stateManager.release(entryPointKey);
        }
    }

    @Subscribe
    public void handle(UpdateEntryPointEvent event) {
        EntryPointKey key = event.getKey();
        try {
            this.stateManager.lock(key);

            Optional<EntryPoint> existingConfiguration = Optional.ofNullable(
                    stateManager.getPendingConfiguration(key)
                            .orElseGet(() -> stateManager.getCommittingConfiguration(key)
                                    .orElseGet(() -> stateManager.getCurrentConfiguration(key)
                                            .orElse(null)))
            );

            existingConfiguration.map(c -> c.mergeWithUpdate(event.getUpdatedEntryPoint()))
                    .ifPresent(c -> {
                        Optional<EntryPoint> preparedConfiguration = stateManager.prepare(key, c);
                        if (preparedConfiguration.isPresent()) {
                            outputBus.post(new EntryPointUpdatedEvent(event.getCorrelationId(), key, preparedConfiguration.get()));
                        }
                    });

        } finally {
            this.stateManager.release(key);
        }
    }

    @Subscribe
    public void handle(SwapAvailabilityRequestedEvent swapAvailabilityRequestedEvent) {
        EntryPointKey key = swapAvailabilityRequestedEvent.getKey();
        try {
            this.stateManager.lock(key);
            boolean isDisabled = this.stateManager.isDisabled(swapAvailabilityRequestedEvent.getKey());
            this.stateManager.setDisabled(key, !isDisabled);
            outputBus.post(new AvailabilitySwappedEvent(swapAvailabilityRequestedEvent.getCorrelationId(), key, true));
        } catch (Throwable throwable) {
            LOGGER.error("can't change availability of entrypoint " + key, throwable);
        } finally {
            this.stateManager.release(key);
            outputBus.post(new AvailabilitySwappedEvent(swapAvailabilityRequestedEvent.getCorrelationId(), key, false));
        }
    }

    @Subscribe
    public void handle(RegisterServerEvent event) {
        LOGGER.info("receive event {}", event);
        EntryPointKey key = event.getKey();
        try {
            this.stateManager.lock(key);
            Optional<EntryPoint> existingConfiguration = Optional.ofNullable(
                    stateManager.getPendingConfiguration(key)
                            .orElseGet(() -> stateManager.getCommittingConfiguration(key)
                                    .orElseGet(() -> stateManager.getCurrentConfiguration(key)
                                            .orElse(null)))
            );

            existingConfiguration.map(c -> c.registerServers(event.getBackend(), event.getServers()))
                    .ifPresent(c -> {
                        Optional<EntryPoint> preparedConfiguration = stateManager.prepare(key, c);

                        if (preparedConfiguration.isPresent()) {
                            LOGGER.info("new servers registered for EntryPoint {}", event.getKey().getID());
                            if (LOGGER.isDebugEnabled()) {
                                for (IncomingEntryPointBackendServer server : event.getServers()) {
                                    LOGGER.debug("- registered server {}", server);
                                }
                            }
                            ServerRegisteredEvent serverRegisteredEvent = new ServerRegisteredEvent(event.getCorrelationId(), event.getKey(), event.getBackend(), event.getServers());
                            LOGGER.debug("post to event bus event {}", serverRegisteredEvent);
                            outputBus.post(serverRegisteredEvent);
                        } else {
                            LOGGER.warn("can't prepare configuration on key {}", key);
                        }
                    });

        } finally {
            this.stateManager.release(key);
        }
    }

    @Subscribe
    public void handle(TryCommitCurrentConfigurationEvent event) {
        EntryPointKey entryPointKey = event.getKey();
        try {
            this.stateManager.lock(entryPointKey);
            Optional<EntryPoint> entryPoint = stateManager.tryCommitCurrent(event.getCorrelationId(), entryPointKey);
            if (entryPoint.isPresent()) {
                EntryPoint configuration = entryPoint.get();
                if (haproxyRepository.getDisabledHaproxyIds()
                        .orElseThrow(() -> new IllegalStateException("can't retrieve haproxy ids for entrypoint " + entryPointKey))
                        .contains(configuration.getHaproxy()) || stateManager.isDisabled(entryPointKey)) {
                    stateManager.cancelCommit(entryPointKey);
                    LOGGER.info("skip tryCommitCurrent for event {} because haproxy {} is disabled", event, configuration.getHaproxy());
                } else {
                    String template = templateLocator.readTemplate(configuration).orElseThrow(() -> new RuntimeException("Could not find any template for configuration " + entryPointKey));
                    Map<String, Integer> portsMapping = getOrCreatePortsMapping(entryPointKey, configuration);
                    String conf = templateGenerator.generate(template, configuration, portsMapping);
                    String syslogConf = templateGenerator.generateSyslogFragment(configuration, portsMapping);
                    CommitRequestedEvent commitRequestedEvent = new CommitRequestedEvent(event.getCorrelationId(), entryPointKey, configuration, conf, syslogConf);
                    LOGGER.debug("from handle -> post to event bus event {}", commitRequestedEvent);
                    outputBus.post(commitRequestedEvent);
                }
            }
        } finally {
            this.stateManager.release(entryPointKey);
        }
    }

    @Subscribe
    public void handle(TryCommitPendingConfigurationEvent event) {
        EntryPointKey entryPointKey = event.getKey();
        try {
            this.stateManager.lock(entryPointKey);
            Optional<EntryPoint> entryPoint = stateManager.tryCommitPending(event.getCorrelationId(), entryPointKey);
            if (entryPoint.isPresent()) {
                EntryPoint configuration = entryPoint.get();
                // TODO use cache for retrieving disabled haproxy
                if (haproxyRepository.getDisabledHaproxyIds()
                        .orElseThrow(() -> new IllegalStateException("can't retrieve haproxy ids for entrypoint " + entryPointKey))
                        .contains(configuration.getHaproxy()) || stateManager.isDisabled(entryPointKey)) {
                    stateManager.cancelCommit(entryPointKey);
                    LOGGER.info("skip tryCommitPending for event {} because haproxy {} is disabled", event, configuration.getHaproxy());
                } else {
                    String template = templateLocator.readTemplate(configuration).orElseThrow(() -> new RuntimeException("Could not find any template for configuration " + entryPointKey));
                    Map<String, Integer> portsMapping = getOrCreatePortsMapping(entryPointKey, configuration);
                    String conf = templateGenerator.generate(template, configuration, portsMapping);
                    String syslogConf = templateGenerator.generateSyslogFragment(configuration, portsMapping);
                    CommitRequestedEvent commitRequestedEvent = new CommitRequestedEvent(event.getCorrelationId(), entryPointKey, configuration, conf, syslogConf);
                    LOGGER.debug("from handle -> post to event bus event {}", commitRequestedEvent);
                    outputBus.post(commitRequestedEvent);
                }
            }
        } finally {
            this.stateManager.release(entryPointKey);
        }
    }

    @Subscribe
    public void handle(CommitSuccessEvent event) {
        LOGGER.debug("Handle CommitSuccessEvent");
        EntryPointKey key = event.getKey();
        try {
            this.stateManager.lock(key);
            Optional<String> optionalCorrelationId = stateManager.getCommitCorrelationId(key);
            if (optionalCorrelationId.isPresent() && optionalCorrelationId.get().equals(event.getCorrelationId())) {
                Optional<EntryPoint> currentConfiguration = stateManager.commit(key);
                if (currentConfiguration.isPresent()) {
                    LOGGER.info("Configuration for EntryPoint {} has been committed", event.getKey().getID());
                    outputBus.post(new CommitCompletedEvent(event.getCorrelationId(), key, currentConfiguration.get()));
                }
            }
        } finally {
            this.stateManager.release(key);
        }
    }

    private Map<String, Integer> getOrCreatePortsMapping(EntryPointKey key, EntryPoint entryPoint) {
        Map<String, Integer> portsMapping = new HashMap<>();

        int syslogPort = portRepository.getPort(key, entryPoint.syslogPortId()).orElseGet(() -> portRepository.newPort(key, entryPoint.syslogPortId()));
        portsMapping.put(entryPoint.syslogPortId(), syslogPort);

        for (EntryPointFrontend frontend : entryPoint.getFrontends()) {
            int frontendPort = portRepository.getPort(key, frontend.portId()).orElseGet(() -> portRepository.newPort(key, frontend.portId()));
            portsMapping.put(frontend.portId(), frontendPort);
        }

        return portsMapping;
    }

    @Subscribe
    public void handle(CommitFailureEvent event) {
        EntryPointKey key = event.getKey();
        try {
            this.stateManager.lock(key);
            Optional<String> commitCorrelationId = stateManager.getCommitCorrelationId(key);
            if (commitCorrelationId.isPresent() && commitCorrelationId.get().equals(event.getCorrelationId())) {
                LOGGER.info("Configuration for EntryPoint {} failed. Commit is canceled.", key);
                stateManager.cancelCommit(key);
            } else {
                LOGGER.info("Received a failed event but either there is no committing configuration or the correlation id does not match.");
            }
        } finally {
            this.stateManager.release(key);
        }
    }

    public static class EntryPointEventHandlerBuilder {
        private EntryPointRepository entryPointRepository;
        private HaproxyRepository haproxyRepository;
        private TemplateGenerator templateGenerator;
        private TemplateLocator templateLocator;
        private PortRepository portRepository;
        private int commitTimeout;

        private EntryPointEventHandlerBuilder(EntryPointRepository entryPointRepository, HaproxyRepository haproxyRepository) {
            this.entryPointRepository = entryPointRepository;
            this.haproxyRepository = haproxyRepository;
        }

        public EntryPointEventHandler outputMessagesTo(EventBus eventBus) {
            EntryPointStateManager stateManager = new EntryPointStateManager(commitTimeout, entryPointRepository);
            return new EntryPointEventHandler(stateManager, portRepository, haproxyRepository, templateLocator, templateGenerator, eventBus);
        }

        public EntryPointEventHandlerBuilder findTemplatesWith(TemplateLocator templateLocator) {
            this.templateLocator = templateLocator;
            return this;
        }

        public EntryPointEventHandlerBuilder generatesTemplatesWith(TemplateGenerator templateGenerator) {
            this.templateGenerator = templateGenerator;
            return this;
        }

        public EntryPointEventHandlerBuilder getPortsWith(PortRepository portRepository) {
            this.portRepository = portRepository;
            return this;
        }

        public EntryPointEventHandlerBuilder commitTimeoutIn(int commitTimeout) {
            this.commitTimeout = commitTimeout;
            return this;
        }
    }

}
