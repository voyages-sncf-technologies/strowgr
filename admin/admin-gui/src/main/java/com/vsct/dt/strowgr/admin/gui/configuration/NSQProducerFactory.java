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
 * <p>
 * Created by william_montaz on 16/02/2016.
 */
public class NSQProducerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(NSQProducerFactory.class);

    @NotEmpty
    private String host;

    @JsonProperty
    @Min(1)
    @Max(65535)
    private int tcpPort;

    @JsonProperty
    @Min(1)
    @Max(65535)
    private int httpPort;

    @JsonProperty
    public String getHost() {
        return host;
    }

    @JsonProperty
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Get TCP port.
     *
     * @deprecated use {@link NSQProducerFactory#getTcpPort}
     */
    @JsonProperty
    public int getPort() {
        LOGGER.warn("Seems 'port' parameter is used for NSQ Producer. It's deprecated, use 'tcp_port' instead of.");
        return tcpPort;
    }

    /**
     * Set TCP port.
     *
     * @param port tcp port
     * @deprecated use {@link NSQProducerFactory#setTcpPort}
     */
    @Deprecated
    @JsonProperty
    public void setPort(int port) {
        LOGGER.warn("Seems 'port' parameter is used for NSQ Producer. It's deprecated, use 'tcp_port' instead of.");
        this.tcpPort = port;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public void setTcpPort(int tcpPort) {
        this.tcpPort = tcpPort;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public NSQProducer build() {
        NSQProducer nsqProducer = new NSQProducer();
        nsqProducer.addAddress(getHost(), getTcpPort());
        LOGGER.info("read NSQ Producer configuration with host:{}, port: {}", getHost(), getTcpPort());
        return nsqProducer;
    }

}
