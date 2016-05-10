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
                        if (preparedConfiguration.isPresent()) {
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
    public void handle(TryCommitCurrentConfigurationEvent event) {
        EntryPointKey key = event.getKey();
        try {
            this.stateManager.lock(key);
            Optional<EntryPoint> committingConfiguration = stateManager.tryCommitCurrent(event.getCorrelationId(), key);
            if(committingConfiguration.isPresent()) {
                EntryPoint configuration = committingConfiguration.get();
                String template = templateLocator.readTemplate(configuration);
                Map<String, Integer> portsMapping = getOrCreatePortsMapping(key, configuration);
                String conf = templateGenerator.generate(template, configuration, portsMapping);
                String syslogConf = templateGenerator.generateSyslogFragment(configuration, portsMapping);
                CommitBeginEvent commitBeginEvent = new CommitBeginEvent(event.getCorrelationId(), key, configuration, conf, syslogConf);
                LOGGER.debug("from handle -> post to event bus event {}", commitBeginEvent);
                outputBus.post(commitBeginEvent);
            }
        } finally {
            this.stateManager.release(key);
        }
    }

    @Subscribe
    public void handle(TryCommitPendingConfigurationEvent event) {
        EntryPointKey key = event.getKey();
        try {
            this.stateManager.lock(key);
            Optional<EntryPoint> committingConfiguration = stateManager.tryCommitPending(event.getCorrelationId(), key);
            if (committingConfiguration.isPresent()) {
                EntryPoint configuration = committingConfiguration.get();
                String template = templateLocator.readTemplate(configuration);
                Map<String, Integer> portsMapping = getOrCreatePortsMapping(key, configuration);
                String conf = templateGenerator.generate(template, configuration, portsMapping);
                String syslogConf = templateGenerator.generateSyslogFragment(configuration, portsMapping);
                CommitBeginEvent commitBeginEvent = new CommitBeginEvent(event.getCorrelationId(), key, configuration, conf, syslogConf);
                LOGGER.debug("from handle -> post to event bus event {}", commitBeginEvent);
                outputBus.post(commitBeginEvent);
            }
        } finally {
            this.stateManager.release(key);
        }
    }

    @Subscribe
    public void handle(CommitSuccessEvent event) {
        EntryPointKey key = event.getKey();
        try {
            this.stateManager.lock(key);
            Optional<String> optionalCorrelationId = stateManager.getCommitCorrelationId(key);
            if(optionalCorrelationId.isPresent() && optionalCorrelationId.get().equals(event.getCorrelationId())) {
                Optional<EntryPoint> currentConfiguration = stateManager.commit(key);
                if (currentConfiguration.isPresent()) {
                    LOGGER.info("Configuration for EntryPoint {} has been committed", event.getKey().getID());
                    outputBus.post(new CommitCompleteEvent(event.getCorrelationId(), key, currentConfiguration.get()));
                }
            }
        } finally {
            this.stateManager.release(key);
        }
    }

    private Map<String, Integer> getOrCreatePortsMapping(EntryPointKey key, EntryPoint entryPoint) {
        Map<String, Integer> portsMapping = new HashMap<>();

        int syslogPort = portProvider.getPort(key, entryPoint.syslogPortId()).orElseGet(() -> portProvider.newPort(key, entryPoint.syslogPortId()));
        portsMapping.put(entryPoint.syslogPortId(), syslogPort);

        for (EntryPointFrontend frontend : entryPoint.getFrontends()) {
            int frontendPort = portProvider.getPort(key, frontend.portId()).orElseGet(() -> portProvider.newPort(key, frontend.portId()));
            portsMapping.put(frontend.portId(), frontendPort);
        }

        return portsMapping;
    }

    @Subscribe
    public void handle(CommitFailureEvent event) {
        EntryPointKey key = event.getKey();
        try{
            this.stateManager.lock(key);
            Optional<String> commitCorrelationId = stateManager.getCommitCorrelationId(key);
            if(commitCorrelationId.isPresent() && commitCorrelationId.get().equals(event.getCorrelationId())){
                LOGGER.info("Configuration for EntryPoint {} failed. Commit is canceled.");
                stateManager.cancelCommit(key);
            } else {
                LOGGER.info("Received a faield event but either there is no committing configuration or the correlation id does not match.");
            }
        } finally {
            this.stateManager.release(key);
        }
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
