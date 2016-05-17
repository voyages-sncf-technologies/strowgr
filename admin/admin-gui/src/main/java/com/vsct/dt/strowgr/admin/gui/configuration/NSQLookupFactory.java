package com.vsct.dt.strowgr.admin.gui.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.brainlag.nsq.lookup.DefaultNSQLookup;
import com.github.brainlag.nsq.lookup.NSQLookup;
import io.dropwizard.setup.Environment;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * Created by william_montaz on 16/02/2016.
 */
public class NSQLookupFactory {

    @NotEmpty
    private String host;

    @Min(1)
    @Max(65535)
    private int port;

    @JsonProperty
    public String getHost() {
        return host;
    }

    @JsonProperty
    public void setHost(String host) {
        this.host = host;
    }

    @JsonProperty
    public int getPort() {
        return port;
    }

    @JsonProperty
    public void setPort(int port) {
        this.port = port;
    }

    public NSQLookup build(Environment environment) {
        NSQLookup lookup = new DefaultNSQLookup();
        lookup.addLookupAddress(getHost(), getPort());
        return lookup;
    }

}
