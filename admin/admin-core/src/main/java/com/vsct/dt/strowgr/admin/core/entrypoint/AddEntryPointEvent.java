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
package com.vsct.dt.strowgr.admin.core.entrypoint;

import com.vsct.dt.strowgr.admin.core.EntryPointKey;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.core.event.in.EntryPointEvent;
import com.vsct.dt.strowgr.admin.core.rx.EventObserver;

import java.util.Objects;

/**
 * Event of a new added entry point.
 */
public abstract class AddEntryPointEvent extends EntryPointEvent implements EventObserver<AddEntryPointResponse> {

    private final EntryPoint configuration;

    public AddEntryPointEvent(String correlationId, EntryPointKey key, EntryPoint configuration) {
        super(correlationId, key);
        this.configuration = Objects.requireNonNull(configuration);
    }

    public EntryPoint getConfiguration() {
        return configuration;
    }

    @Override
    public String toString() {
        return "AddEntryPointEvent{correlationId=" + getCorrelationId() + "key=" + getKey() + "configuration=" + configuration + "}";
    }

}
