package com.vsct.dt.strowgr.admin.core;

import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class helps to manage the EntryPoint repository
 * and provides an abstraction of the state of an EntryPoint
 */
public class EntryPointStateManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntryPointStateManager.class);
    private final int commitTimeout;
    private final EntryPointRepository repository;

    EntryPointStateManager(int commitTimeout, EntryPointRepository repository) {
        this.repository = checkNotNull(repository);
        this.commitTimeout = commitTimeout;
    }

    /**
     * Provides exclusive access to an EntryPoint on the repository
     *
     * @param key of the entrypoint
     */
    void lock(EntryPointKey key) {
        this.repository.lock(key);
    }

    /**
     * Release exclusive access of an EntryPoint
     *
     * @param key of the entrypoint
     */
    void release(EntryPointKey key) {
        this.repository.release(key);
    }

    Optional<EntryPoint> getCurrentConfiguration(EntryPointKey key) {
        return repository.getCurrentConfiguration(key);
    }

    Optional<EntryPoint> getPendingConfiguration(EntryPointKey key) {
        return repository.getPendingConfiguration(key);
    }

    Optional<EntryPoint> getCommittingConfiguration(EntryPointKey key) {
        return repository.getCommittingConfiguration(key);
    }

    /**
     * Puts a configuration in pending state.
     * The configuration will be pending only if it is different
     * from possible existing committing or current configuration
     *
     * @param key           of the entrypoint
     * @param configuration of the entrypoint
     * @return the new pending configuration (optional)
     */
    Optional<EntryPoint> prepare(EntryPointKey key, EntryPoint configuration) {
        Optional<EntryPoint> committingConfiguration = repository.getCommittingConfiguration(key);

        if (committingConfiguration.isPresent()) {
            if (!committingConfiguration.get().equals(configuration)) {
                repository.setPendingConfiguration(key, configuration);
                return Optional.of(configuration);
            }
        } else {
            Optional<EntryPoint> currentConfiguration = repository.getCurrentConfiguration(key);
            if (currentConfiguration.isPresent()) {
                if (!currentConfiguration.get().equals(configuration)) {
                    repository.setPendingConfiguration(key, configuration);
                    return Optional.of(configuration);
                }
            } else {
                repository.setPendingConfiguration(key, configuration);
                return Optional.of(configuration);
            }
        }

        return Optional.empty();
    }

    /**
     * Put the pending configuration in committing state, only if there is not already a configuration in committing state
     *
     *
     * @param correlationId
     * @param key of the entrypoint
     * @return the new committing configuration (optional)
     */
    Optional<EntryPoint> tryCommitPending(String correlationId, EntryPointKey key) {
        Optional<EntryPoint> pendingConfiguration = repository.getPendingConfiguration(key);

        if (pendingConfiguration.isPresent()) {
            if (repository.getCommittingConfiguration(key).isPresent()) {
                LOGGER.debug("can't committing a new pending configuration, there is already one in commit phase.");
            } else {
                repository.setCommittingConfiguration(correlationId, key, pendingConfiguration.get(), commitTimeout);
                repository.removePendingConfiguration(key);
                return pendingConfiguration;
            }
        } else {
            LOGGER.trace("no pending configuration for key {}", key);
        }

        return Optional.empty();
    }

    /**
     * Put the current configuration in committing state, only if there is not already a configuration in committing state
     *
     *
     * @param correlationId
     * @param key of the entrypoint
     * @return the new committing configuration (optional)
     */
    Optional<EntryPoint> tryCommitCurrent(String correlationId, EntryPointKey key) {
        Optional<EntryPoint> currentConfiguration = repository.getCurrentConfiguration(key);

        if (currentConfiguration.isPresent()) {
            if (!repository.getCommittingConfiguration(key).isPresent()) {
                repository.setCommittingConfiguration(correlationId, key, currentConfiguration.get(), commitTimeout);
                return currentConfiguration;
            } else {
                LOGGER.debug("can't committing a new current configuration, there is already one in commit phase.");
            }
        } else {
            LOGGER.debug("can't find current configuration for entrypoint with key {}", key);
        }
        return Optional.empty();
    }

    /**
     * Sets the committing configuration as the new current configuration, only if there was an existing committing configuration
     *
     * @param key of the entrypoint
     * @return the new current configuration (optional)
     */
    Optional<EntryPoint> commit(EntryPointKey key) {
        Optional<EntryPoint> committingConfiguration = repository.getCommittingConfiguration(key);

        if (committingConfiguration.isPresent()) {
            repository.setCurrentConfiguration(key, committingConfiguration.get());
            repository.removeCommittingConfiguration(key);
            return committingConfiguration;
        }
        return Optional.empty();
    }

    /**
     * Removes the committing configuration
     * @param key of the entrypoint
     * @return the current configuration, if available
     */
    public Optional<EntryPoint> cancelCommit(EntryPointKey key) {
        repository.removeCommittingConfiguration(key);
        return this.getCommittingConfiguration(key);
    }

    /**
     * Returns the correlation id that led to a commit action
     * @param key of the entrypoint
     * @return an Optional of String. The Optional is empty if there is no committing configuration at all
     */
    public Optional<String> getCommitCorrelationId(EntryPointKey key) {
        return repository.getCommitCorrelationId(key);
    }
}
