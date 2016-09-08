/*
 *  Copyright (C) 2016 VSCT
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.vsct.dt.strowgr.admin.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQConfig;
import com.github.brainlag.nsq.NSQProducer;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.google.common.eventbus.*;
import com.vsct.dt.strowgr.admin.core.EntryPointEventHandler;
import com.vsct.dt.strowgr.admin.core.TemplateGenerator;
import com.vsct.dt.strowgr.admin.gui.cli.ConfigurationCommand;
import com.vsct.dt.strowgr.admin.gui.cli.InitializationCommand;
import com.vsct.dt.strowgr.admin.gui.configuration.StrowgrConfiguration;
import com.vsct.dt.strowgr.admin.gui.healthcheck.ConsulHealthcheck;
import com.vsct.dt.strowgr.admin.gui.healthcheck.NsqHealthcheck;
import com.vsct.dt.strowgr.admin.gui.manager.ConsumableHAPTopics;
import com.vsct.dt.strowgr.admin.gui.manager.NSQProducerManager;
import com.vsct.dt.strowgr.admin.gui.resource.api.EntrypointResources;
import com.vsct.dt.strowgr.admin.gui.resource.api.HaproxyResources;
import com.vsct.dt.strowgr.admin.gui.resource.api.PortResources;
import com.vsct.dt.strowgr.admin.gui.subscribers.EventBusSubscriber;
import com.vsct.dt.strowgr.admin.gui.tasks.HaproxyVipTask;
import com.vsct.dt.strowgr.admin.gui.tasks.InitPortsTask;
import com.vsct.dt.strowgr.admin.nsq.NSQ;
import com.vsct.dt.strowgr.admin.nsq.consumer.RegisterServerConsumer;
import com.vsct.dt.strowgr.admin.nsq.producer.NSQDispatcher;
import com.vsct.dt.strowgr.admin.nsq.producer.NSQHttpClient;
import com.vsct.dt.strowgr.admin.repository.consul.ConsulRepository;
import com.vsct.dt.strowgr.admin.template.IncompleteConfigurationException;
import com.vsct.dt.strowgr.admin.template.generator.MustacheTemplateGenerator;
import com.vsct.dt.strowgr.admin.template.locator.UriTemplateLocator;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;
import rx.internal.schedulers.ExecutorScheduler;
import rx.schedulers.Schedulers;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

public class StrowgrMain extends Application<StrowgrConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StrowgrMain.class);

    public static void main(String[] args) throws Exception {
        new StrowgrMain().run(args);
    }

    @Override
    public String getName() {
        return "strowgr";
    }

    @Override
    public void initialize(Bootstrap<StrowgrConfiguration> strowgrConfiguration) {
        super.initialize(strowgrConfiguration);

        strowgrConfiguration.addBundle(new AssetsBundle("/assets", "/", "index.html"));
        strowgrConfiguration.addBundle(new AssetsBundle("/META-INF/resources/webjars", "/webjars", null, "webjars"));

        strowgrConfiguration.addCommand(new InitializationCommand());
        strowgrConfiguration.addCommand(new ConfigurationCommand());
    }

    @Subscribe
    public void handleDeadEvent(DeadEvent deadEvent) {
        LOGGER.error("an event has no subscribers: {}", deadEvent);
    }

    @Override
    public void run(StrowgrConfiguration configuration, Environment environment) throws Exception {
        LOGGER.info("start dropwizard configuration");

        /* Allow the use of another NSQ channel for development purposes */
        if(configuration.getNsqChannel() != null){
            NSQ.CHANNEL = configuration.getNsqChannel();
        }

        /* Main EventBus */
        BlockingQueue eventBusQueue = new ArrayBlockingQueue<>(100);
        ExecutorService executor = environment.lifecycle().executorService("main-bus-handler-threads").workQueue(eventBusQueue).minThreads(configuration.getThreads()).maxThreads(configuration.getThreads()).build();

        /* Executor wrapper for RXjava */
        Scheduler executorScheduler = new ExecutorScheduler(executor);

        EventBus eventBus = new AsyncEventBus(executor, (exception, context) -> {
            LOGGER.error("exception on main event bus. Context: " + subscriberExceptionContextToString(context), exception);
        });
        eventBus.register(this); // for dead events

        /* Templates */
        TemplateGenerator templateGenerator = new MustacheTemplateGenerator();
        UriTemplateLocator templateLocator = new UriTemplateLocator();

        /* Repository */
        ConsulRepository repository = configuration.getConsulRepositoryFactory().buildAndManageBy(environment);

        /* EntryPoint State Machine */
        EntryPointEventHandler eventHandler = EntryPointEventHandler
                .backedBy(repository)
                .getPortsWith(repository)
                .findTemplatesWith(templateLocator)
                .generatesTemplatesWith(templateGenerator)
                .commitTimeoutIn(configuration.getCommitTimeout())
                .outputMessagesTo(eventBus);

        eventBus.register(eventHandler);

        /* NSQ Consumers */
        //Object mapper used for NSQ messages
        ObjectMapper objectMapper = new ObjectMapper();
        // retrieve NSQLookup configuration
        NSQLookup nsqLookup = configuration.getNsqLookupfactory().build();
        //NSQConsumers configuration
        NSQConfig consumerNsqConfig = configuration.getNsqConsumerConfigFactory().build();

        RegisterServerConsumer registerServerConsumer = new RegisterServerConsumer(nsqLookup, objectMapper, consumerNsqConfig);
        Observable registerServerObservable = registerServerConsumer.observe();

        // This managed resource will properly handle consumers and their related observables depending on repository configuration
        ConsumableHAPTopics consumableTopics = new ConsumableHAPTopics(repository, nsqLookup, consumerNsqConfig, objectMapper, configuration.getHandledHaproxyRefreshPeriodSecond());
        Observable hapTopicsObservable = consumableTopics.observe();

        Observable nsqEventsObservable = hapTopicsObservable.mergeWith(registerServerObservable);

        //Push all nsq events to eventBus
        //We observeOn a single thread to avoid blocking nio eventloops
        //NSQToEventBusSubscriber applies backpressure in regard to the eventBusQueue
        nsqEventsObservable.observeOn(Schedulers.newThread()).subscribe(new EventBusSubscriber(eventBus, eventBusQueue));

        /* Manage resources */
        environment.lifecycle().manage(consumableTopics);
        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() throws Exception {
            }

            @Override
            public void stop() throws Exception {
                registerServerConsumer.shutdown();
            }
        });

        /* NSQ Producers */
        NSQProducer nsqProducer = configuration.getNsqProducerFactory().build();
        // manage NSQProducer lifecycle by Dropwizard
        environment.lifecycle().manage(new NSQProducerManager(nsqProducer));
        // Pipeline from eventbus to NSQ producer
        eventBus.register(new ToNSQSubscriber(new NSQDispatcher(nsqProducer)));

        /* Commit schedulers */
        configuration.getPeriodicSchedulerFactory().getPeriodicCommitCurrentSchedulerFactory().build(repository, eventBus::post, environment);
        configuration.getPeriodicSchedulerFactory().getPeriodicCommitPendingSchedulerFactory().build(repository, eventBus::post, environment);

        /* REST Resources */
        EntrypointResources restApiResource = new EntrypointResources(eventBus, repository);
        environment.jersey().register(restApiResource);

        HaproxyResources haproxyResources = new HaproxyResources(repository, templateLocator, templateGenerator);
        environment.jersey().register(haproxyResources);

        PortResources portResources = new PortResources(repository);
        environment.jersey().register(portResources);

        eventBus.register(restApiResource);

        /* Http Client */
        CloseableHttpClient httpClient = new HttpClientBuilder(environment)
                .using(configuration.getHttpClientConfiguration())
                .build("http-client");
        NSQHttpClient nsqdHttpClient = new NSQHttpClient("http://" + configuration.getNsqProducerFactory().getHost() + ":" + configuration.getNsqProducerFactory().getHttpPort(), httpClient);
        NSQHttpClient nsqLookupdHttpClient = new NSQHttpClient("http://" + configuration.getNsqLookupfactory().getHost() + ":" + configuration.getNsqLookupfactory().getPort(), httpClient);

        /* Healthchecks */
        environment.healthChecks().register("nsqlookup", new NsqHealthcheck(nsqLookupdHttpClient));
        environment.healthChecks().register("nsqproducer", new NsqHealthcheck(nsqdHttpClient));
        environment.healthChecks().register("consul", new ConsulHealthcheck(configuration.getConsulRepositoryFactory().getHost(), configuration.getConsulRepositoryFactory().getPort()));

        /* admin */
        environment.admin().addTask(new InitPortsTask(repository));
        environment.admin().addTask(new HaproxyVipTask(repository));

        /* Exception mappers */
        environment.jersey().register(new ExceptionMapper<IncompleteConfigurationException>() {
            @Override
            public Response toResponse(IncompleteConfigurationException e) {
                return Response.status(500).entity(e.getMessage()).type(MediaType.TEXT_PLAIN_TYPE).build();
            }
        });
    }

    private String subscriberExceptionContextToString(SubscriberExceptionContext subscriberExceptionContext) {
        return "event: " + subscriberExceptionContext.getEvent() +
                ", eventbus identifier: " + subscriberExceptionContext.getEventBus().identifier() +
                ", subscriber: " + subscriberExceptionContext.getSubscriber() +
                ", subscriber method name: " + subscriberExceptionContext.getSubscriberMethod().getName();
    }

}
