package com.vsct.dt.haas.admin.gui.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.vsct.dt.haas.admin.core.event.in.RegisterServerEvent;
import com.vsct.dt.haas.admin.nsq.consumer.RegisterServerConsumer;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Created by william_montaz on 16/02/2016.
 */
public class RegisterServerMessageConsumerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterServerMessageConsumerFactory.class);

    @NotEmpty
    private String topic;

    @JsonProperty("topic")
    public String getTopic() {
        return topic;
    }

    @JsonProperty("topic")
    public void setTopic(String topic) {
        this.topic = topic;
    }

    public RegisterServerConsumer build(NSQLookup lookup, Consumer<RegisterServerEvent> consumer, Environment environment){
        RegisterServerConsumer registerServerConsumer = new RegisterServerConsumer(getTopic(), lookup, consumer);
        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() throws Exception {
                LOGGER.info("Starting RegisterServerMessageConsumer");
                registerServerConsumer.start();
            }

            @Override
            public void stop() throws Exception {
                LOGGER.info("Stopping RegisterServerMessageConsumer");
                registerServerConsumer.stop();
            }
        });
        return registerServerConsumer;
    }
}
