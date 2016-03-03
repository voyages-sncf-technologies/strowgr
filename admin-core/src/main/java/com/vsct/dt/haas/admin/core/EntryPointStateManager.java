package com.vsct.dt.haas.admin.core;

import com.google.common.base.Preconditions;
import com.vsct.dt.haas.admin.core.configuration.EntryPointConfiguration;

import java.util.Optional;
import java.util.Set;

/**
 * This class helps to manage the EntryPoint repository
 * and provides an abstraction of the state of an EntryPoint
 */
public class EntryPointStateManager {

    private final int                 commitTimeout;
    private final EntryPointRepository repository;

    EntryPointStateManager(int commitTimeout, EntryPointRepository repository) {
        Preconditions.checkNotNull(repository);
        this.repository = repository;
        this.commitTimeout = commitTimeout;
    }

    /**
     * Provides exclusive access to an EntryPoint on the repository
     *
     * @param key
     */
    void lock(EntryPointKey key) {
        this.repository.lock(key);
    }

    /**
     * Release exclusive access of an EntryPoint
     *
     * @param key
     */
    void release(EntryPointKey key) {
        this.repository.release(key);
    }

    Set<String> getManagedEntryPointsIds() {
        return repository.getEntryPointsId();
    }

    Optional<EntryPointConfiguration> getCurrentConfiguration(EntryPointKey key) {
        return repository.getCurrentConfiguration(key);
    }

    Optional<EntryPointConfiguration> getPendingConfiguration(EntryPointKey key) {
        return repository.getPendingConfiguration(key);
    }

    Optional<EntryPointConfiguration> getCommittingConfiguration(EntryPointKey key) {
        return repository.getCommittingConfiguration(commitTimeout, key);
    }

    /**
     * Puts a configuration in pending state.
     * The configuration will be pending only if it is different
     * from possible existing committing or current configuration
     *
     * @param key
     * @param configuration
     * @return the new pending configuration (optional)
     */
    Optional<EntryPointConfiguration> prepare(EntryPointKey key, EntryPointConfiguration configuration) {
        Optional<EntryPointConfiguration> committingConfiguration = repository.getCommittingConfiguration(commitTimeout, key);

        if (committingConfiguration.isPresent()) {
            if (!committingConfiguration.get().equals(configuration)) {
                repository.setPendingConfiguration(key, configuration);
                return Optional.of(configuration);
            }
        }
        else {
            Optional<EntryPointConfiguration> currentConfiguration = repository.getCurrentConfiguration(key);
            if (currentConfiguration.isPresent()) {
                if (!currentConfiguration.get().equals(configuration)) {
                    repository.setPendingConfiguration(key, configuration);
                    return Optional.of(configuration);
                }
            }
            else {
                repository.setPendingConfiguration(key, configuration);
                return Optional.of(configuration);
            }
        }

        return Optional.empty();
    }

    /**
     * Put the pending configuration in committing state, only if there is not already a configuration in committing state
     *
     * @param key
     * @return the new committing configuration (optional)
     */
    Optional<EntryPointConfiguration> tryCommitPending(EntryPointKey key) {
        Optional<EntryPointConfiguration> pendingConfiguration = repository.getPendingConfiguration(key);

        if (pendingConfiguration.isPresent()) {
            if (!repository.getCommittingConfiguration(commitTimeout, key).isPresent()) {
                repository.setCommittingConfiguration(key, pendingConfiguration.get());
                repository.removePendingConfiguration(key);
                return pendingConfiguration;
            }
        }
        return Optional.empty();
    }

    /**
     * Put the current configuration in committing state, only if there is not already a configuration in committing state
     *
     * @param key
     * @return the new committing configuration (optional)
     */
    Optional<EntryPointConfiguration> tryCommitCurrent(EntryPointKey key) {
        Optional<EntryPointConfiguration> currentConfiguration = repository.getCurrentConfiguration(key);

        if (currentConfiguration.isPresent()) {
            if (!repository.getCommittingConfiguration(commitTimeout, key).isPresent()) {
                repository.setCommittingConfiguration(key, currentConfiguration.get());
                return currentConfiguration;
            }
        }
        return Optional.empty();
    }

    /**
     * Sets the committing configuration as the new current configuration, only if there was an existing committing configuration
     *
     * @param key
     * @return the new current configuration (optional)
     */
    Optional<EntryPointConfiguration> commit(EntryPointKey key) {
        Optional<EntryPointConfiguration> committingConfiguration = repository.getCommittingConfiguration(commitTimeout, key);

        if (committingConfiguration.isPresent()) {
            repository.setCurrentConfiguration(key, committingConfiguration.get());
            repository.removeCommittingConfiguration(key);
            return committingConfiguration;
        }
        return Optional.empty();
    }

}
