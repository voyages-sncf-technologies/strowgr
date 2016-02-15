package com.vsct.dt.haas.admin.nsq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQConsumer;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.vsct.dt.haas.admin.core.event.in.CommitSuccessEvent;

import java.io.IOException;
import java.util.function.Consumer;

public class CommitMessageConsumer {

    private static final String CHANNEL = "admin";
    private static final String SUCCESS_TOPIC_PREFIX = "commit_complete_";
    private final NSQConsumer successCommitConsumer;
    private final ObjectMapper mapper = new ObjectMapper();

    public CommitMessageConsumer(NSQLookup lookup, String haproxy, Consumer<CommitSuccessEvent> consumer) {
        successCommitConsumer = new NSQConsumer(lookup, SUCCESS_TOPIC_PREFIX + haproxy, CHANNEL, (message) -> {

            CommitCompletePayload payload = null;
            try {
                payload = mapper.readValue(message.getMessage(), CommitCompletePayload.class);
            } catch (IOException e) {
                e.printStackTrace();
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
        successCommitConsumer.start();
    }

    public void stop() {
        successCommitConsumer.shutdown();
    }

}
