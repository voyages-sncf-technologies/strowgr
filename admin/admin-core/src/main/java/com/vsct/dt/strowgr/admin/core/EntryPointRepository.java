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

import java.util.Map;
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
     * Get vip for a given haproxy id.
     * @param id of the haproxy
     * @return return Optional String of the vip, {@code Optional#empty} if haproxy can't be found.
     */
    Optional<String> getHaproxyVip(String id);

    /**
     * Get haproxy ids stored in repository.
     * @return Set of haproxy ids
     */
    Optional<Set<String>> getHaproxyIds();

    /**
     * Set a property for haproxy. For instance a vip, a haproxyId etc...
     *
     * @param haproxyId of the haproxy
     * @param key to set for this haproxy
     * @param value for this key
     */
    void setHaproxyProperty(String haproxyId, String key, String value);

    /**
     * Get a property for haproxy.
     *
     * @param haproxyId of the haproxy
     * @param key to set for this haproxy
     */
    Optional<String> getHaproxyProperty(String haproxyId, String key);

    Optional<String> getCommitCorrelationId(EntryPointKey key);

    /**
     * Get haproxy properties (vip, name, etc...) for an given id.
     * @param haproxyId id of the haproxy
     * @return haproxy properties map
     */
    Optional<Map<String, Map<String, String>>> getHaproxyProperties(String haproxyId);
}
