/*
 * Copyright (C) 2016 VSCT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.vsct.dt.strowgr.admin.nsq.payload;

import com.vsct.dt.strowgr.admin.nsq.payload.fragment.Header;

public abstract class AbstractPayload {
    private final Header header;

    protected AbstractPayload(String correlationId, String application, String platform) {
        header = new Header(correlationId,application,platform);
    }

    public Header getHeader() {
        return header;
    }

}
