package com.vsct.dt.haas.admin.core;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vsct.dt.haas.admin.core.configuration.EntryPoint;
import com.vsct.dt.haas.admin.core.configuration.EntryPointFrontend;
import com.vsct.dt.haas.admin.core.configuration.IncomingEntryPointBackendServer;
import com.vsct.dt.haas.admin.core.event.in.*;
import com.vsct.dt.haas.admin.core.event.out.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class EntryPointEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryPointEventHandler.class);

    private final EntryPointStateManager stateManager;
    private final EventBus outputBus;
    private final TemplateGenerator templateGenerator;
    private final TemplateLocator templateLocator;
    private final PortProvider portProvider;

    EntryPointEventHandler(EntryPointStateManager stateManager, PortProvider portProvider, TemplateLocator templateLocator, TemplateGenerator templateGenerator, EventBus outputBus) {
        this.stateManager = stateManager;
        this.outputBus = outputBus;
        this.portProvider = portProvider;
        this.templateLocator = templateLocator;
        this.templateGenerator = templateGenerator;
    }

    public static EntryPointEventHandlerBuilder backedBy(EntryPointRepository repository) {
        return new EntryPointEventHandlerBuilder(repository);
    }

    @Subscribe
    public void handle(AddEntryPointEvent event) {
        EntryPointKey key = event.getKey();
        try {
            this.stateManager.lock(key);
            if (!stateManager.getCommittingConfiguration(key).isPresent() && !stateManager.getCurrentConfiguration(key).isPresent()) {
                Optional<EntryPoint> preparedConfiguration = stateManager.prepare(key, event.getConfiguration().orElseThrow(() -> new IllegalStateException("can't retrieve configuration of event " + event)));

                if (preparedConfiguration.isPresent()) {
                    EntryPointAddedEvent entryPointAddedEvent = new EntryPointAddedEvent(event.getCorrelationId(), key, preparedConfiguration.get());
                    LOGGER.info("from handle AddEntryPointEvent new EntryPoint {} added -> {}", key.getID(), entryPointAddedEvent);
                    outputBus.post(entryPointAddedEvent);
                }
            }
        } finally {
            this.stateManager.release(key);
        }
    }

    @Subscribe
    public void handle(UpdateEntryPointEvent event){
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
                        if(preparedConfiguration.isPresent()){
                            outputBus.post(new EntryPointUpdatedEvent(event.getCorrelationId(), key, preparedConfiguration.get()));
                        }
                    });

        } finally {
            this.stateManager.release(key);
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
                            LOGGER.warn("can't prepare configurtion on key {}", key);
                        }
                    });

        } finally {
            this.stateManager.release(key);
        }
    }

    @Subscribe
    public void handleTryCommitCurrentConfigurationEvent(TryCommitCurrentConfigurationEvent event) {
        EntryPointKey key = event.getKey();
        try {
            this.stateManager.lock(key);
            EntryPoint committingConfiguration = stateManager
                    .tryCommitCurrent(key)
                    .orElseThrow(() -> new IllegalStateException("can't commit entrypoint " + key));
            String template = templateLocator.readTemplate(committingConfiguration);
            Map<String, Integer> portsMapping = getOrCreatePortsMapping(key, committingConfiguration);
            String conf = templateGenerator.generate(template, committingConfiguration, portsMapping);
            String syslogConf = templateGenerator.generateSyslogFragment(committingConfiguration, portsMapping);
            CommitBeginEvent commitBeginEvent = new CommitBeginEvent(event.getCorrelationId(), key, committingConfiguration, conf, syslogConf);
            LOGGER.debug("from handleTryCommitCurrentConfigurationEvent -> post to event bus event {}", commitBeginEvent);
            outputBus.post(commitBeginEvent);
        } finally {
            this.stateManager.release(key);
        }
    }

    @Subscribe
    public void handleTryCommitPendingConfigurationEvent(TryCommitPendingConfigurationEvent event) {
        EntryPointKey key = event.getKey();
        try {
            this.stateManager.lock(key);
            Optional<EntryPoint> pendingConfiguration = stateManager.tryCommitPending(key);
            if (pendingConfiguration.isPresent()) {
                String template = templateLocator.readTemplate(pendingConfiguration.get());
                Map<String, Integer> portsMapping = getOrCreatePortsMapping(key, pendingConfiguration.get());
                String conf = templateGenerator.generate(template, pendingConfiguration.get(), portsMapping);
                String syslogConf = templateGenerator.generateSyslogFragment(pendingConfiguration.get(), portsMapping);
                CommitBeginEvent commitBeginEvent = new CommitBeginEvent(event.getCorrelationId(), key, pendingConfiguration.get(), conf, syslogConf);
                LOGGER.debug("from handleTryCommitPendingConfigurationEvent -> post to event bus event {}", commitBeginEvent);
                outputBus.post(commitBeginEvent);
            } else {
                LOGGER.debug("no pending configuring is currently in commit phase");
            }
        } finally {
            this.stateManager.release(key);
        }
    }

    @Subscribe
    public void handleCommitSuccessEvent(CommitSuccessEvent event) {
        EntryPointKey key = event.getKey();
        try {
            this.stateManager.lock(key);
            Optional<EntryPoint> currentConfiguration = stateManager.commit(key);
            if (currentConfiguration.isPresent()) {
                LOGGER.info("Configuration for EntryPoint {} has been committed", event.getKey().getID());
                outputBus.post(new CommitCompleteEvent(event.getCorrelationId(), key, currentConfiguration.get()));
            }
        } finally {
            this.stateManager.release(key);
        }
    }

    private Map<String, Integer> getOrCreatePortsMapping(EntryPointKey key, EntryPoint entryPoint) {
        Map<String, Integer> portsMapping = new HashMap<>();
        String prefix = key.getID() + '-';

        int syslogPort = portProvider.getPort(prefix + entryPoint.syslogPortId()).orElseGet(() -> portProvider.newPort(prefix + entryPoint.syslogPortId()));
        portsMapping.put(entryPoint.syslogPortId(), syslogPort);

        for (EntryPointFrontend frontend : entryPoint.getFrontends()) {
            int frontendPort = portProvider.getPort(prefix + frontend.portId()).orElseGet(() -> portProvider.newPort(prefix + frontend.portId()));
            portsMapping.put(frontend.portId(), frontendPort);
        }

        return portsMapping;
    }

    public static class EntryPointEventHandlerBuilder {
        private EntryPointRepository repository;
        private TemplateGenerator templateGenerator;
        private TemplateLocator templateLocator;
        private PortProvider portProvider;
        private int commitTimeout;

        private EntryPointEventHandlerBuilder(EntryPointRepository repository) {
            this.repository = repository;
        }

        public EntryPointEventHandler outputMessagesTo(EventBus eventBus) {
            EntryPointStateManager stateManager = new EntryPointStateManager(commitTimeout, repository);
            return new EntryPointEventHandler(stateManager, portProvider, templateLocator, templateGenerator, eventBus);
        }

        public EntryPointEventHandlerBuilder findTemplatesWith(TemplateLocator templateLocator) {
            this.templateLocator = templateLocator;
            return this;
        }

        public EntryPointEventHandlerBuilder generatesTemplatesWith(TemplateGenerator templateGenerator) {
            this.templateGenerator = templateGenerator;
            return this;
        }

        public EntryPointEventHandlerBuilder getPortsWith(PortProvider portProvider) {
            this.portProvider = portProvider;
            return this;
        }

        public EntryPointEventHandlerBuilder commitTimeoutIn(int commitTimeout) {
            this.commitTimeout = commitTimeout;
            return this;
        }
    }

}
