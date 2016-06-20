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

import java.util.Map;
import java.util.Optional;

/**
 * Created by william_montaz on 26/02/2016.
 */
public interface PortProvider {

    Optional<Map<String, Integer>> getPorts();

    Optional<Integer> getPort(String key);

    default Optional<Integer> getPort(EntryPointKey key, String portId){
        return getPort(PortProvider.getPortKey(key, portId));
    }

    Integer newPort(String key);

    default Integer newPort(EntryPointKey key, String portId){
        return newPort(PortProvider.getPortKey(key, portId));
    }

    static String getPortKey(EntryPointKey key, String portId) {
        return key.getID() + '-' + portId;
    }

}
