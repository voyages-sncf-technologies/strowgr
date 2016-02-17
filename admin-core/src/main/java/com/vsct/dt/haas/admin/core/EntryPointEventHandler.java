package com.vsct.dt.haas.admin.core;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vsct.dt.haas.admin.core.configuration.EntryPointBackendServer;
import com.vsct.dt.haas.admin.core.configuration.EntryPointConfiguration;
import com.vsct.dt.haas.admin.core.event.in.*;
import com.vsct.dt.haas.admin.core.event.out.CommitBeginEvent;
import com.vsct.dt.haas.admin.core.event.out.CommitCompleteEvent;
import com.vsct.dt.haas.admin.core.event.out.EntryPointAddedEvent;
import com.vsct.dt.haas.admin.core.event.out.ServerRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.Optional;

public class EntryPointEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryPointEventHandler.class);

    private final EntryPointStateManager stateManager;
    private final EventBus outputBus;
    private final TemplateGenerator templateGenerator;
    private final TemplateLocator templateLocator;

    EntryPointEventHandler(EntryPointStateManager stateManager, TemplateLocator templateLocator, TemplateGenerator templateGenerator, EventBus outputBus) {
        this.stateManager = stateManager;
        this.outputBus = outputBus;
        this.templateLocator = templateLocator;
        this.templateGenerator = templateGenerator;
    }

    public static EntryPointEventHandlerBuilder backedBy(EntryPointRepository repository) {
        EntryPointStateManager stateManager = new EntryPointStateManager(repository);
        return new EntryPointEventHandlerBuilder(stateManager);
    }

    @Subscribe
    public void handle(AddEntryPointEvent event) {
        EntryPointKey key = event.getKey();
        try {
            this.stateManager.lock(key);
            if (!stateManager.getCommittingConfiguration(key).isPresent() && !stateManager.getCurrentConfiguration(key).isPresent()) {
                Optional<EntryPointConfiguration> preparedConfiguration = stateManager.prepare(key, event.getConfiguration());

                if (preparedConfiguration.isPresent()) {
                    LOGGER.info("new EntryPoint {} added", key.getID());
                    outputBus.post(new EntryPointAddedEvent(event.getCorrelationId(), key, preparedConfiguration.get()));
                }
            }
        } finally {
            this.stateManager.release(key);
        }
    }

    public void handle(UpdateEntryPointEvent event) {
        /* TODO DEFINE */
    }

    @Subscribe
    public void handle(RegisterServerEvent event) {
        EntryPointKey key = event.getKey();
        try {
            this.stateManager.lock(key);
            Optional<EntryPointConfiguration> existingConfiguration = Optional.ofNullable(
                    stateManager.getPendingConfiguration(key)
                            .orElse(stateManager.getCommittingConfiguration(key)
                                    .orElse(stateManager.getCurrentConfiguration(key)
                                            .orElse(null)))
            );

            existingConfiguration.map(c -> c.registerServers(event.getBackend(), event.getServers()))
                    .ifPresent(c -> {
                        Optional<EntryPointConfiguration> preparedConfiguration = stateManager.prepare(key, c);

                        if (preparedConfiguration.isPresent()) {
                            LOGGER.info("new servers registered for EntryPoint {}", event.getKey().getID());
                            if (LOGGER.isDebugEnabled()) {
                                for (EntryPointBackendServer server : event.getServers()) {
                                    LOGGER.debug("- registered server {}", server);
                                }
                            }
                            outputBus.post(new ServerRegisteredEvent(event.getCorrelationId(), event.getKey(), event.getBackend(), event.getServers()));
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
            Optional<EntryPointConfiguration> committingConfiguration = stateManager.tryCommitCurrent(key);
            if (committingConfiguration.isPresent()) {
                String template = templateLocator.readTemplate(committingConfiguration.get());
                String conf = templateGenerator.generate(template, committingConfiguration.get());
                outputBus.post(new CommitBeginEvent(event.getCorrelationId(), key, committingConfiguration.get(), conf));
            }
        } finally {
            this.stateManager.release(key);
        }
    }

    @Subscribe
    public void handleTryCommitPendingConfigurationEvent(TryCommitPendingConfigurationEvent event) {
        EntryPointKey key = event.getKey();
        try {
            this.stateManager.lock(key);
            Optional<EntryPointConfiguration> committingConfiguration = stateManager.tryCommitPending(key);
            if (committingConfiguration.isPresent()) {
                String template = templateLocator.readTemplate(committingConfiguration.get());
                String conf = templateGenerator.generate(template, committingConfiguration.get());
                outputBus.post(new CommitBeginEvent(event.getCorrelationId(), key, committingConfiguration.get(), conf));
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
            Optional<EntryPointConfiguration> currentConfiguration = stateManager.commit(key);
            if (currentConfiguration.isPresent()) {
                LOGGER.info("Configuration for EntryPoint {} has been committed", event.getKey().getID());
                outputBus.post(new CommitCompleteEvent(event.getCorrelationId(), key, currentConfiguration.get()));
            }
        } finally {
            this.stateManager.release(key);
        }
    }

    public void handleCommitFailureEvent(CommitFailureEvent event) {
        /* TODO DEFINE */
    }

    public void handleCommitTimeoutEvent() {
        /* TODO DEFINE */
    }

    public static class EntryPointEventHandlerBuilder {
        private EntryPointStateManager stateManager;
        private TemplateGenerator templateGenerator;
        private TemplateLocator templateLocator;

        private EntryPointEventHandlerBuilder(EntryPointStateManager stateManager) {
            this.stateManager = stateManager;
        }

        public EntryPointEventHandler outputMessagesTo(EventBus eventBus) {
            return new EntryPointEventHandler(stateManager, templateLocator, templateGenerator, eventBus);
        }

        public EntryPointEventHandlerBuilder findTemplatesWith(TemplateLocator templateLocator) {
            this.templateLocator = templateLocator;
            return this;
        }

        public EntryPointEventHandlerBuilder generatesTemplatesWith(TemplateGenerator templateGenerator) {
            this.templateGenerator = templateGenerator;
            return this;
        }
    }

}
