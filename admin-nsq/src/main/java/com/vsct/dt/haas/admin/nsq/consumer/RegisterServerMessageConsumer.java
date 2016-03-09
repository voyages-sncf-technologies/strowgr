package com.vsct.dt.haas.admin.nsq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQConsumer;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.google.common.collect.Sets;
import com.vsct.dt.haas.admin.core.configuration.EntryPointBackendServer;
import com.vsct.dt.haas.admin.core.configuration.IncomingEntryPointBackendServer;
import com.vsct.dt.haas.admin.core.event.in.RegisterServerEvent;

import java.io.IOException;
import java.util.HashMap;
import java.util.function.Consumer;

public class RegisterServerMessageConsumer {

    private static final String CHANNEL = "admin";
    private final NSQConsumer registerServerConsumer;
    private final ObjectMapper mapper = new ObjectMapper();

    public RegisterServerMessageConsumer(String topic, NSQLookup lookup, Consumer<RegisterServerEvent> consumer) {

        registerServerConsumer = new NSQConsumer(lookup, topic, CHANNEL, (message) -> {

            RegisterServerPayload payload = null;
            try {
                payload = mapper.readValue(message.getMessage(), RegisterServerPayload.class);
            } catch (IOException e) {
                e.printStackTrace();
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

    public void start() {
        registerServerConsumer.start();
    }

    public void stop() {
        registerServerConsumer.shutdown();
    }

}
