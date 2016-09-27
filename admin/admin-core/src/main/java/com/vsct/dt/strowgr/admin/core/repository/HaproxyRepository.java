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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface HaproxyRepository {

    /**
     * Get vip for a given haproxy id.
     * @param id of the haproxy
     * @return return Optional String of the vip, {@code Optional#empty} if haproxy can't be found.
     */
    Optional<String> getHaproxyVip(String id);

    /**
     * Get all haproxy properties for each haproxy
     * @return Map with haproxy properties by haproxy id
     */
    Optional<List<Map<String, String>>> getHaproxyProperties();

    /**
     * Check if this haproxy is on autoreload mode or not.
     * @return true if is on autoreload mode
     * @param haproxyId id of the haproxy
     */
    boolean isAutoreload(String haproxyId);

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
     * @param key of this haproxy property
     */
    Optional<String> getHaproxyProperty(String haproxyId, String key);

    /**
     * Get haproxy properties (vip, name, etc...) for an given id.
     * @param haproxyId id of the haproxy
     * @return haproxy properties map
     */
    Optional<Map<String, String>> getHaproxyProperties(String haproxyId);
}
