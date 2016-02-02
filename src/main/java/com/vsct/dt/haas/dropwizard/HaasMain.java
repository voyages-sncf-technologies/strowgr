package com.vsct.dt.haas.dropwizard;


import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQConsumer;
import com.github.brainlag.nsq.lookup.DefaultNSQLookup;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.google.common.eventbus.EventBus;
import com.vsct.dt.haas.dropwizard.resources.RestApiResources;
import com.vsct.dt.haas.events.AddNewServerEvent;
import com.vsct.dt.haas.events.CommitedEntryPointEvent;
import com.vsct.dt.haas.events.EntryPointDeployedEvent;
import com.vsct.dt.haas.nsq.CommitedEntryPointPayload;
import com.vsct.dt.haas.nsq.EntryPointDeployedPayload;
import com.vsct.dt.haas.nsq.NewServerPayload;
import com.vsct.dt.haas.state.AdminState;
import com.vsct.dt.haas.state.Server;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class HaasMain extends Application<HaasConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HaasMain.class);

    ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        new HaasMain().run(args);
    }

    @Override
    public String getName() {
        return "haas";
    }

    @Override
    public void initialize(Bootstrap<HaasConfiguration> haasConfiguration) {
        super.initialize(haasConfiguration);

        haasConfiguration.addBundle(new AssetsBundle("/assets", "/", "index.html"));
        haasConfiguration.addBundle(new AssetsBundle("/META-INF/resources/webjars", "/webjars", null, "webjars"));
    }

    @Override
    public void run(HaasConfiguration haasConfiguration, Environment environment) throws Exception{
        MetricRegistry metricRegistry = environment.metrics();

        EventBus eventBus = new EventBus();
        AdminState adminState = new AdminState();

        eventBus.register(adminState);
        RestApiResources restApiResource = new RestApiResources(adminState, eventBus);

        environment.jersey().register(restApiResource);

        //The NSQ Consumers
        NSQLookup lookup = new DefaultNSQLookup();
        lookup.addLookupAddress("floradora", 50161);
        NSQConsumer consumer1 = new NSQConsumer(lookup, "entrypoint_deployed_default-name", "admin", (message) -> {

            EntryPointDeployedPayload payload = null;
            try {
                payload = objectMapper.readValue(message.getMessage(), EntryPointDeployedPayload.class);
            } catch (IOException e) {
                e.printStackTrace();
            }

            EntryPointDeployedEvent event = new EntryPointDeployedEvent(payload.application, payload.platform);
            eventBus.post(event);
        });

        NSQConsumer consumer2 = new NSQConsumer(lookup, "new_server_default-name", "admin", (message) -> {

            NewServerPayload payload = null;
            try {
                payload = objectMapper.readValue(message.getMessage(), NewServerPayload.class);
            } catch (IOException e) {
                e.printStackTrace();
            }

            AddNewServerEvent event = new AddNewServerEvent(payload.application, payload.platform, payload.backend, new Server(payload.instanceName, payload.name, payload.ip, payload.port));
            eventBus.post(event);
        });

        NSQConsumer consumer3 = new NSQConsumer(lookup, "updated_entrypoint_default-name", "admin", (message) -> {

            CommitedEntryPointPayload payload = null;
            try {
                payload = objectMapper.readValue(message.getMessage(), CommitedEntryPointPayload.class);
            } catch (IOException e) {
                e.printStackTrace();
            }

            CommitedEntryPointEvent event = new CommitedEntryPointEvent(payload.application, payload.platform);
            eventBus.post(event);
        });

        consumer1.start();
        consumer2.start();
        consumer3.start();

    }

}
