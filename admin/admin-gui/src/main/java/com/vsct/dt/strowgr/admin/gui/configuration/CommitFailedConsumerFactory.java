package com.vsct.dt.strowgr.admin.gui.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQConsumer;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.vsct.dt.strowgr.admin.core.event.in.CommitFailureEvent;
import com.vsct.dt.strowgr.admin.nsq.consumer.CommitFailedPayload;
import com.vsct.dt.strowgr.admin.nsq.consumer.EntryPointKeyVsctImpl;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Configuration factory from Dropwizard for CommitFailedConsumer NSQ.
 */
public class CommitFailedConsumerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommitFailedConsumerFactory.class);

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

    private final ObjectMapper mapper = new ObjectMapper();

    public NSQConsumer build(NSQLookup lookup, String haproxy, Consumer<CommitFailureEvent> consumer) {
        return new NSQConsumer(lookup, topic + haproxy, "admin", (message) -> {
            CommitFailedPayload payload = null;
            try {
                payload = mapper.readValue(message.getMessage(), CommitFailedPayload.class);
            } catch (IOException e) {
                LOGGER.error("can't deserialize the payload:" + Arrays.toString(message.getMessage()), e);
                //Avoid republishing message and stop processing
                message.finished();
                return;
            }

            CommitFailureEvent event = new CommitFailureEvent(payload.getCorrelationId(), new EntryPointKeyVsctImpl(payload.getApplication(), payload.getPlatform()));
            consumer.accept(event);
            message.finished();
        });
    }

}
