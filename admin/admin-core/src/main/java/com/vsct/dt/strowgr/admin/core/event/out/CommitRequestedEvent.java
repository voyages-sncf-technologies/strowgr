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
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.core.event.in.EntryPointEvent;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class CommitRequestedEvent extends EntryPointEvent {
    private final EntryPoint configuration;
    private final String conf;
    private final String syslogConf;
    private final String bind;

    public CommitRequestedEvent(String correlationId, EntryPointKey key, EntryPoint configuration, String conf, String syslogConf, String bind) {
        super(correlationId, key);
        this.configuration = checkNotNull(configuration);
        this.conf = checkNotNull(conf);
        this.syslogConf = checkNotNull(syslogConf);
        this.bind = checkNotNull(bind);
    }

    public EntryPoint getConfiguration() {
        return configuration;
    }

    public String getConf() {
        return conf;
    }

    public String getSyslogConf() {
        return syslogConf;
    }

    public String getBind() {
        return bind;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommitRequestedEvent that = (CommitRequestedEvent) o;
        return Objects.equals(configuration, that.configuration) &&
                Objects.equals(conf, that.conf) &&
                Objects.equals(syslogConf, that.syslogConf) &&
                Objects.equals(bind, that.bind);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configuration, conf, syslogConf, bind);
    }

    @Override
    public String toString() {
        return "CommitRequestedEvent{" +
                "configuration=" + configuration +
                ", conf='" + conf + '\'' +
                ", syslogConf='" + syslogConf + '\'' +
                ", bind='" + bind + '\'' +
                '}';
    }
}
