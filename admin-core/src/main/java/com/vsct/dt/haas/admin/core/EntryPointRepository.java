package com.vsct.dt.haas.admin.core;

import com.vsct.dt.haas.admin.core.configuration.EntryPoint;

import java.util.Optional;
import java.util.Set;

/**
 * An admin repository stores configurations of an admin and convenient methods to handle them
 */
public interface EntryPointRepository {

    Optional<EntryPoint> getCurrentConfiguration(EntryPointKey key);

    Optional<EntryPoint> getPendingConfiguration(EntryPointKey key);

    Optional<EntryPoint> getCommittingConfiguration(EntryPointKey key);

    void setPendingConfiguration(EntryPointKey key, EntryPoint configuration);

    void removePendingConfiguration(EntryPointKey key);

    /**
     * Sets the committing configuration with a TTL
     * @param key of the entrypoint
     * @param configuration conent of the entrypoint
     * @param ttl the ttl in seconds
     */
    void setCommittingConfiguration(String correlationId, EntryPointKey key, EntryPoint configuration, int ttl);

    void removeCommittingConfiguration(EntryPointKey key);

    void setCurrentConfiguration(EntryPointKey key, EntryPoint configuration);

    Set<String> getEntryPointsId();

    void lock(EntryPointKey key);

    void release(EntryPointKey key);

    Optional<String> getHaproxyVip(String haproxyName);

    Optional<String> getCommitCorrelationId(EntryPointKey key);

}
