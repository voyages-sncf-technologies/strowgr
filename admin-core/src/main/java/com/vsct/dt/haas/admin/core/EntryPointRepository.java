package com.vsct.dt.haas.admin.core;

import com.vsct.dt.haas.admin.core.configuration.EntryPointConfiguration;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * An admin repository stores configurations of an admin and convenient methods to handle them
 */
public interface EntryPointRepository {

    Optional<EntryPointConfiguration> getCurrentConfiguration(EntryPointKey key);

    Optional<EntryPointConfiguration> getPendingConfiguration(EntryPointKey key);

    Optional<EntryPointConfiguration> getCommittingConfiguration(EntryPointKey key);

    void setPendingConfiguration(EntryPointKey key, EntryPointConfiguration configuration);

    void removePendingConfiguration(EntryPointKey key);

    /**
     * Sets the committing configuration with a TTL
     * @param key
     * @param configuration
     * @param ttl the ttl in seconds
     */
    void setCommittingConfiguration(EntryPointKey key, EntryPointConfiguration configuration, int ttl);

    void removeCommittingConfiguration(EntryPointKey key);

    void setCurrentConfiguration(EntryPointKey key, EntryPointConfiguration configuration);

    Set<String> getEntryPointsId();

    void lock(EntryPointKey key);

    public void release(EntryPointKey key);

}
