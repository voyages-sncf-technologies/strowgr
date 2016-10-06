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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Conf content of an entry point.
 * Mainly for Haproxy and Syslog configuration content.
 */
public class Conf {
    /**
     * Haproxy configuration file resolved from template and based64 encoded.
     */
    private final String haproxy;

    /**
     * Syslog configuration file resolved from template and based64 encoded.
     */
    private final String syslog;

    /**
     * On what does this entrypoint binds to
     */
    private final String bind;

    @JsonCreator
    public Conf(@JsonProperty("haproxy") String haproxy, @JsonProperty("syslog") String syslog, @JsonProperty("bind") String bind) {
        this.haproxy = checkNotNull(haproxy);
        this.syslog = checkNotNull(syslog);
        this.bind = checkNotNull(bind);
    }

    public String getHaproxy() {
        return haproxy;
    }

    public String getSyslog() {
        return syslog;
    }

    public String getBind() {
        return bind;
    }
}
