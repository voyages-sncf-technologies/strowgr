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

import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;

import java.io.IOException;
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
     * @return {@link Boolean#TRUE} if entrypoint has been removed from repository, {@link Boolean#FALSE} if entrypoint has not been found, null otherwise
     */
    Boolean removeEntrypoint(EntryPointKey entryPointKey);

    void setCurrentConfiguration(EntryPointKey key, EntryPoint configuration);

    Set<String> getEntryPointsId();

    boolean lock(EntryPointKey key);

    void release(EntryPointKey key);

    Optional<String> getHaproxyVip(String haproxyName);

    Optional<String> getCommitCorrelationId(EntryPointKey key);

}
