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

package com.vsct.dt.strowgr.admin.nsq.payload.fragment;

import com.fasterxml.jackson.annotation.JsonProperty;

import static com.google.common.base.Preconditions.checkNotNull;

public class Conf {
    @JsonProperty("haproxy")
    private String haproxy;
    @JsonProperty("syslog")
    private String syslog;

    public Conf(String haproxy, String syslog) {
        this.haproxy = checkNotNull(haproxy);
        this.syslog = checkNotNull(syslog);
    }

    public String getHaproxy() {
        return haproxy;
    }

    public void setHaproxy(String haproxy) {
        this.haproxy = haproxy;
    }

    public String getSyslog() {
        return syslog;
    }

    public void setSyslog(String syslog) {
        this.syslog = syslog;
    }
}