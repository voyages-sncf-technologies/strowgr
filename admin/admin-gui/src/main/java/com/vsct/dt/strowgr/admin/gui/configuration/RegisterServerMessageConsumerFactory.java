package com.vsct.dt.strowgr.admin.gui.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQConsumer;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.google.common.collect.Sets;
import com.vsct.dt.strowgr.admin.core.configuration.IncomingEntryPointBackendServer;
import com.vsct.dt.strowgr.admin.core.event.in.RegisterServerEvent;
import com.vsct.dt.strowgr.admin.nsq.consumer.EntryPointKeyVsctImpl;
import com.vsct.dt.strowgr.admin.nsq.consumer.RegisterServerPayload;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Configuration factory from Dropwizard for RegisterServerMessageConsumer NSQ.
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

    private final ObjectMapper mapper = new ObjectMapper();

    public NSQConsumer build(NSQLookup lookup, Consumer<RegisterServerEvent> consumer){
        return new NSQConsumer(lookup, topic, "admin", (message) -> {

            RegisterServerPayload payload = null;
            try {
                payload = mapper.readValue(message.getMessage(), RegisterServerPayload.class);
                if (payload.getCorrelationId() == null) {
                    payload.setCorrelationId(Arrays.toString(message.getId()));
                }
                if (payload.getTimestamp() == null) {
                    payload.setTimestamp(message.getTimestamp().getTime());
                }
            } catch (IOException e) {
                LOGGER.error("can't deserialize the payload of message at " + message.getTimestamp() + ", id=" + Arrays.toString(message.getId()) + ": " + Arrays.toString(message.getMessage()), e);
                //Avoid republishing message and stop processing
                message.finished();
                return;
            }

            /* TODO Use some conflation to prevent dispatching all event */
            RegisterServerEvent event = new RegisterServerEvent(payload.getCorrelationId(),
                    new EntryPointKeyVsctImpl(payload.getApplication(), payload.getPlatform()),
                    payload.getBackend(),
                    Sets.newHashSet(new IncomingEntryPointBackendServer(payload.getId(), payload.getHostname(), payload.getIp(), payload.getPort(), payload.getContext())));

            consumer.accept(event);

            message.finished();
        });
    }
}
