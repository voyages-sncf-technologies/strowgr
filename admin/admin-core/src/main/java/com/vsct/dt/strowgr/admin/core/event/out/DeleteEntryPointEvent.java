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
package com.vsct.dt.strowgr.admin.core.event.out;

import com.vsct.dt.strowgr.admin.core.EntryPointKey;
import com.vsct.dt.strowgr.admin.core.event.in.EntryPointEvent;

/**
 * Event from admin which is triggered when admin has removed an entrypoint from its repository.
 */
public class DeleteEntryPointEvent extends EntryPointEvent {

    private final String haproxyName, application, platform;

    public DeleteEntryPointEvent(String correlationId, EntryPointKey key, String haproxyName, String application, String platform) {
        super(correlationId, key);
        this.haproxyName = haproxyName;
        this.application = application;
        this.platform = platform;
    }

    public String getPlatform() {
        return platform;
    }

    public String getApplication() {
        return application;
    }

    public String getHaproxyName() {
        return haproxyName;
    }
}
