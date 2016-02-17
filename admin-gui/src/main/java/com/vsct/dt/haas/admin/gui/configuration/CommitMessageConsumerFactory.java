package com.vsct.dt.haas.admin.gui.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.vsct.dt.haas.admin.core.event.in.CommitSuccessEvent;
import com.vsct.dt.haas.admin.nsq.consumer.CommitMessageConsumer;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Created by william_montaz on 16/02/2016.
 */
public class CommitMessageConsumerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommitMessageConsumerFactory.class);

    @NotEmpty
    private String topic;

    @JsonProperty("successTopic")
    public String getTopic() {
        return topic;
    }

    @JsonProperty("successTopic")
    public void setTopic(String topic) {
        this.topic = topic;
    }

    public CommitMessageConsumer build(NSQLookup lookup, String haproxy, Consumer<CommitSuccessEvent> consumer, Environment environment) {
        CommitMessageConsumer commitMessageConsumer = new CommitMessageConsumer(getTopic(), lookup, haproxy, consumer);
        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() throws Exception {
                LOGGER.info("Starting CommitMessageConsumer");
                commitMessageConsumer.start();
            }

            @Override
            public void stop() throws Exception {
                LOGGER.info("Stopping CommitMessageConsumer");
                commitMessageConsumer.stop();
            }
        });
        return commitMessageConsumer;
    }

}
