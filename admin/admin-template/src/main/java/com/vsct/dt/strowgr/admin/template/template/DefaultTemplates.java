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

package com.vsct.dt.strowgr.admin.template.template;

import java.util.StringJoiner;

public class DefaultTemplates {
    public static final String SYSLOG_DEFAULT_TEMPLATE = new StringJoiner("\n")
            .add("source s_{{application}}_{{platform}} { udp(ip(127.0.0.1) port({{syslog_port}})); };")
            .add("destination d_{{application}}_{{platform}} { file(\"/HOME/hapadm/{{application}}/logs/{{application}}{{platform}}/haproxy.log\", perm(0664)); };")
            .add("log { source(s_{{application}}_{{platform}}); filter(f_local0); destination(d_{{application}}_{{platform}}); };")
            .add("\n")
            .toString();
}
