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
package com.vsct.dt.strowgr.admin.nsq.consumer;

import com.vsct.dt.strowgr.admin.core.EntryPointKey;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An entry point is defined by application and platform
 */
public class EntryPointKeyVsctImpl implements EntryPointKey {

    private final String application;
    private final String platform;

    public EntryPointKeyVsctImpl(String application, String platform) {
        checkNotNull(application);
        checkNotNull(platform);
        checkArgument(application.length() > 0, "Application should not be empty");
        checkArgument(platform.length() > 0, "Platform should not be empty");
        this.application = application;
        this.platform = platform;
    }

    /**
     * Build an {@link EntryPointKeyVsctImpl} from an id.
     *
     * @param id of the entrypoint (for example: 'PAO/REL1')
     * @return a new {@link EntryPointKeyVsctImpl}
     */
    public static EntryPointKeyVsctImpl fromID(String id) {
        checkNotNull(id);
        checkArgument(id.length() > 0, "id should not be empty");
        checkArgument(id.contains("/"));
        String[] splitted = id.split("/");
        return new EntryPointKeyVsctImpl(splitted[0], splitted[1]);
    }

    public String getApplication() {
        return application;
    }

    public String getPlatform() {
        return platform;
    }

    @Override
    public String getID() {
        return application + "/" + platform;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntryPointKeyVsctImpl that = (EntryPointKeyVsctImpl) o;

        boolean applicationEq = (application != null ?
                application.equals(that.application) :
                that.application == null);
        boolean platformEq = (platform != null ?
                platform.equals(that.platform) :
                that.platform == null);

        return applicationEq && platformEq;

    }

    @Override
    public int hashCode() {
        int result = application != null ? application.hashCode() : 0;
        result = 31 * result + (platform != null ? platform.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return getID();
    }
}
