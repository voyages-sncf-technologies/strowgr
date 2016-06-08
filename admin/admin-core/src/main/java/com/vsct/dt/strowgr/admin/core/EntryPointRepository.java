package com.vsct.dt.strowgr.admin.core;

import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;

import java.util.Optional;
import java.util.Set;

/**
 * An admin repository stores configurations of an admin and convenient methods to handle them
 */
public interface EntryPointRepository {

    /**
     * Get current configuration for a given entrypoint from the repository.
     *
     * @param key of the entrypoint
     * @return the optional of the entrypoint, Optional.empty() if the query has failed. Must not be null.
     */
    Optional<EntryPoint> getCurrentConfiguration(EntryPointKey key);

    Optional<EntryPoint> getPendingConfiguration(EntryPointKey key);

    Optional<EntryPoint> getCommittingConfiguration(EntryPointKey key);

    void setPendingConfiguration(EntryPointKey key, EntryPoint configuration);

    void removePendingConfiguration(EntryPointKey key);

    /**
     * Sets the committing configuration with a TTL
     *
     * @param key           of the entrypoint
     * @param configuration conent of the entrypoint
     * @param ttl           the ttl in seconds
     */
    void setCommittingConfiguration(String correlationId, EntryPointKey key, EntryPoint configuration, int ttl);

    void removeCommittingConfiguration(EntryPointKey key);

    /**
     * Remove this entrypoint from Strowgr. This command must be forward to all Strowgr components (sidekick, database...)
     *
     * @param entryPointKey of the entrypoint to delete (ex. MY_APP/PROD1)
     * @return {@code Optional#of(Boolean#TRUE)} if entrypoint has been removed from repository, {@code Optional#of(Boolean#TRUE)} if entrypoint has not been found, Optional.empty() otherwise
     */
    Optional<Boolean> removeEntrypoint(EntryPointKey entryPointKey);

    void setCurrentConfiguration(EntryPointKey key, EntryPoint configuration);

    Set<String> getEntryPointsId();

    boolean lock(EntryPointKey key);

    void release(EntryPointKey key);

    /**
     * Get vip for a given haproxy name.
     * @param name of the haproxy
     * @return return Optional String of the vip, {@code Optional#empty} if haproxy can't be found.
     */
    Optional<String> getHaproxyVip(String name);

    /**
     * Set the vip for a given haproxy name.
     * @param name of the haproxy
     * @param vip to set to this haproxy
     */
    void setHaproxyVip(String name, String vip);

    Optional<String> getCommitCorrelationId(EntryPointKey key);

}
