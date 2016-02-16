package com.vsct.dt.haas.admin.gui.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.admin.nsq.producer.Producer;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
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


    public Producer build(Environment environment) {
        Producer producer = new Producer(getHost(), getPort());
        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() throws Exception {
                LOGGER.info("Starting NSQProducer");
                producer.start();
            }

            @Override
            public void stop() throws Exception {
                LOGGER.info("Stopping NSQProducer");
                producer.stop();
            }
        });
        return producer;
    }

}
