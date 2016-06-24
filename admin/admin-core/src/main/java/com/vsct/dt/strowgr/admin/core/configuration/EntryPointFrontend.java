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

package com.vsct.dt.strowgr.admin.core.configuration;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.vsct.dt.strowgr.admin.Preconditions.checkStringNotEmpty;

public class EntryPointFrontend {

    private final String id;
    private final HashMap<String, String> context;

    public EntryPointFrontend(String id, Map<String, String> context) {
        this.id = checkStringNotEmpty(id, "Frontend should have an id");
        this.context = new HashMap<>(checkNotNull(context));
    }

    public String getId() {
        return id;
    }

    public Map<String, String> getContext() {
        return new HashMap<>(context);
    }

    public String portId() {
        return this.id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntryPointFrontend that = (EntryPointFrontend) o;

        if (context != null ? !context.equals(that.context) : that.context != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (context != null ? context.hashCode() : 0);
        return result;
    }

}
