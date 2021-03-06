/*
 * Copyright (C) 2016 VSCT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vsct.dt.strowgr.admin.core.event.in;

import com.vsct.dt.strowgr.admin.core.EntryPointKey;

/**
 * Generic entrypoint event.
 * <p>
 * Created by william_montaz on 05/02/2016.
 */
public class EntryPointEvent {

    private final String correlationId;
    private final EntryPointKey key;

    public EntryPointEvent(String correlationId, EntryPointKey key) {
        this.correlationId = correlationId;
        this.key = key;
    }

    public EntryPointKey getKey() {
        return key;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public String toString() {
        return "EntryPointEvent{" +
                "correlationId='" + correlationId + '\'' +
                ", key=" + key + '}';
    }
}
