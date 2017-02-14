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
package com.vsct.dt.strowgr.admin.core;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default entrypoint key implementation.
 * <p/>
 * Created by william_montaz on 09/02/2016.
 */
public class EntryPointKeyDefaultImpl implements EntryPointKey {

    private final String id;

    public EntryPointKeyDefaultImpl(String id) {
        checkNotNull(id, "id of an entrypoint can't be null");
        this.id = id;
    }

    @Override
    public String getID() {
        return this.id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntryPointKeyDefaultImpl that = (EntryPointKeyDefaultImpl) o;
        return id != null ? id.equals(that.id) : that.id == null;

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return id;
    }
}
