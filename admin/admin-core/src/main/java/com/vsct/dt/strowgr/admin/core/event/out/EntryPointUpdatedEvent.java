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

package com.vsct.dt.strowgr.admin.core.event.out;

import com.vsct.dt.strowgr.admin.core.EntryPointKey;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.core.event.in.EntryPointEvent;

import java.util.Optional;

/**
 * Created by william_montaz on 12/04/2016.
 */
public class EntryPointUpdatedEvent extends EntryPointEvent {
    private final EntryPoint configuration;

    public EntryPointUpdatedEvent(String correlationId, EntryPointKey key, EntryPoint configuration) {
        super(correlationId, key);
        this.configuration = configuration;
    }

    public Optional<EntryPoint> getConfiguration() {
        return Optional.ofNullable(configuration);
    }
}
