package com.vsct.dt.strowgr.admin.nsq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQConsumer;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.vsct.dt.strowgr.admin.core.event.in.CommitSuccessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

public class CommitCompletedConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommitCompletedConsumer.class);

    private static final String CHANNEL = "admin";
    private final NSQConsumer nsqConsumer;
    private final ObjectMapper mapper = new ObjectMapper();

    public CommitCompletedConsumer(String topic, NSQLookup lookup, String haproxy, Consumer<CommitSuccessEvent> consumer) {
        nsqConsumer = new NSQConsumer(lookup, topic + haproxy, CHANNEL, (message) -> {

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

    public void start() {
        nsqConsumer.start();
    }

    public void stop() {
        nsqConsumer.shutdown();
    }

}
