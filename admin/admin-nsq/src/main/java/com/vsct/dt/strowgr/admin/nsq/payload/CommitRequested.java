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
package com.vsct.dt.strowgr.admin.nsq.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.strowgr.admin.nsq.NSQ;
import com.vsct.dt.strowgr.admin.nsq.payload.fragment.Conf;
import com.vsct.dt.strowgr.admin.nsq.payload.fragment.Header;

import java.util.Objects;

public class CommitRequested {

    private final Header header;
    private final Conf conf;

    @JsonCreator
    public CommitRequested(@JsonProperty("header") Header header, @JsonProperty("conf") Conf conf) {
        this.header = header;
        this.conf = conf;
    }

    public CommitRequested(String correlationId, String application, String platform, String confBase64, String syslogConfBase64, String haproxyVersion, String bind) {
        this(new Header(correlationId, application, platform), new Conf(confBase64, syslogConfBase64, haproxyVersion, bind));
    }

    public Header getHeader() {
        return header;
    }

    public Conf getConf() {
        return conf;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommitRequested that = (CommitRequested) o;
        return Objects.equals(header, that.header) &&
                Objects.equals(conf, that.conf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(header, conf);
    }
}
