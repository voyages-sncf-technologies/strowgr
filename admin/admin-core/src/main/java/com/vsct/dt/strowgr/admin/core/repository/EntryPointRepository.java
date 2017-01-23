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

package com.vsct.dt.strowgr.admin.core.repository;

import com.vsct.dt.strowgr.admin.core.EntryPointKey;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;

import java.io.IOException;
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
     * @param configuration content of the entrypoint
     * @param ttl           the ttl in seconds
     */
    boolean setCommittingConfiguration(String correlationId, EntryPointKey key, EntryPoint configuration, int ttl);

    void removeCommittingConfiguration(EntryPointKey key);

    /**
     * Remove this entrypoint from Strowgr. This command must be forward to all Strowgr components (sidekick, database...)
     *
     * @param entryPointKey of the entrypoint to delete (ex. MY_APP/PROD1)
     * @return {@code Optional#of(Boolean#TRUE)} if entrypoint has been removed from repository, {@code Optional#of(Boolean#TRUE)} if entrypoint has not been found, Optional.empty() otherwise
     */
    Optional<Boolean> removeEntrypoint(EntryPointKey entryPointKey);

    void setCurrentConfiguration(EntryPointKey key, EntryPoint configuration);

    /**
     * Initialize the repository
     *
     * @throws IOException for any problem with repository access
     */
    public void init() throws IOException;

    /**
     * Retrieve entrypoint ids.
     *
     * @return Set of entrypoint ids
     */
    Set<String> getEntryPointsId();

    boolean lock(EntryPointKey key);

    /**
     * Check if the entrypoint is on autoreload mode or not.
     *
     * @param entryPointKey to check
     * @return true if entryPointKey field 'autoreload' is present and valued at 'true'. False otherwise.
     */
    boolean isAutoreloaded(EntryPointKey entryPointKey);

    void release(EntryPointKey key);

    Optional<String> getCommitCorrelationId(EntryPointKey key);

    void setAutoreload(EntryPointKey entryPointKey, Boolean autoreload);

}
