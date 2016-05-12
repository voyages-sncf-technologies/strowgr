package com.vsct.dt.haas.admin.template.template;

import java.util.StringJoiner;

public class DefaultTemplates {
    public static final String SYSLOG_DEFAULT_TEMPLATE = new StringJoiner("\n")
            .add("source s_{{application}}_{{platform}} { udp(ip(127.0.0.1) port({{syslog_port}})); };")
            .add("destination d_{{application}}_{{platform}} { file(\"/HOME/hapadm/{{application}}/logs/{{application}}{{platform}}/haproxy.log\", perm(0664)); };")
            .add("log { source(s_{{application}}_{{platform}}); filter(f_local0); destination(d_{{application}}_{{platform}}); };")
            .add("\n")
            .toString();
}
