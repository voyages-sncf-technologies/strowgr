package com.vsct.dt.haas.admin.gui.configuration;

import com.github.brainlag.nsq.lookup.NSQLookup;
import com.vsct.dt.haas.admin.core.event.in.RegisterServerEvent;
import com.vsct.dt.haas.admin.core.event.out.ServerRegisteredEvent;
import com.vsct.dt.haas.admin.nsq.consumer.RegisterServerMessageConsumer;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Created by william_montaz on 16/02/2016.
 */
public class RegisterServerMessageConsumerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterServerMessageConsumerFactory.class);

    public static RegisterServerMessageConsumer build(NSQLookup lookup, Consumer<RegisterServerEvent> consumer, Environment environment){
        RegisterServerMessageConsumer registerServerMessageConsumer = new RegisterServerMessageConsumer(lookup, consumer);
        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() throws Exception {
                LOGGER.info("Starting RegisterServerMessageConsumer");
                registerServerMessageConsumer.start();
            }

            @Override
            public void stop() throws Exception {
                LOGGER.info("Stopping RegisterServerMessageConsumer");
                registerServerMessageConsumer.stop();
            }
        });
        return registerServerMessageConsumer;
    }
}
