package com.vsct.dt.strowgr.admin.gui.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.brainlag.nsq.NSQProducer;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * NSQProducerFactory for reading NSQProducer configuration from dropwizard yaml.
 *
 * Created by william_montaz on 16/02/2016.
 */
public class NSQProducerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(NSQProducerFactory.class);

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

    public NSQProducer build() {
        NSQProducer nsqProducer = new NSQProducer();
        nsqProducer.addAddress(getHost(), getPort());
        LOGGER.info("read NSQ Producer configuration with host:{}, port: {}", getHost(), getPort());
        return nsqProducer;
    }

}
