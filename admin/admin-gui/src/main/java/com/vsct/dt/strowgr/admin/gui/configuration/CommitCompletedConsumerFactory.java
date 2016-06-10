package com.vsct.dt.strowgr.admin.gui.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQConsumer;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.vsct.dt.strowgr.admin.core.event.in.CommitSuccessEvent;
import com.vsct.dt.strowgr.admin.nsq.consumer.CommitCompletedPayload;
import com.vsct.dt.strowgr.admin.nsq.consumer.EntryPointKeyVsctImpl;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Configuration factory from Dropwizard for CommitCompletedConsumer NSQ.
 * <p>
 * Created by william_montaz on 16/02/2016.
 */
public class CommitCompletedConsumerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommitCompletedConsumerFactory.class);

    // TODO externalize mapper Jackson for a more controlled use of serialization/deserialization
    private final ObjectMapper mapper = new ObjectMapper();

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

    public NSQConsumer build(NSQLookup lookup, String haproxy, Consumer<CommitSuccessEvent> consumer) {
        return new NSQConsumer(lookup, topic + haproxy, "admin", (message) -> {
            CommitCompletedPayload payload = null;
            try {
                payload = mapper.readValue(message.getMessage(), CommitCompletedPayload.class);
            } catch (IOException e) {
                LOGGER.error("can't deserialize the payload:" + Arrays.toString(message.getMessage()), e);
                //Avoid republishing message and stop processing
                message.finished();
                return;
            }

            CommitSuccessEvent event = new CommitSuccessEvent(payload.getCorrelationId(), new EntryPointKeyVsctImpl(payload.getApplication(), payload.getPlatform()));
            consumer.accept(event);
            message.finished();
        });
    }

}
