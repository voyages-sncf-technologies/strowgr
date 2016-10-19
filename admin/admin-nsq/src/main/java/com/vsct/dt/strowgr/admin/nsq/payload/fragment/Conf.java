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

import java.util.Objects;

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

    /**
     * Haproxy version.
     */
    private final String haproxyVersion;

    @JsonCreator
    public Conf(@JsonProperty("haproxy") String haproxy, @JsonProperty("syslog") String syslog, @JsonProperty("haproxyVersion") String haproxyVersion, @JsonProperty("bind") String bind) {
        this.haproxy = checkNotNull(haproxy);
        this.syslog = checkNotNull(syslog);
        this.bind = checkNotNull(bind);
        this.haproxyVersion = checkNotNull(haproxyVersion);
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

    public String getHaproxyVersion() {
        return haproxyVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Conf conf = (Conf) o;
        return Objects.equals(haproxy, conf.haproxy) &&
                Objects.equals(syslog, conf.syslog) &&
                Objects.equals(bind, conf.bind) &&
                Objects.equals(haproxyVersion, conf.haproxyVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(haproxy, syslog, bind, haproxyVersion);
    }
}
