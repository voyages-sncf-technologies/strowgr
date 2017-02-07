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
import com.google.common.eventbus.*;
import com.vsct.dt.strowgr.admin.core.*;
import com.vsct.dt.strowgr.admin.core.entrypoint.*;
import com.vsct.dt.strowgr.admin.core.event.CorrelationId;
import com.vsct.dt.strowgr.admin.core.event.in.*;
import com.vsct.dt.strowgr.admin.core.event.out.CommitRequestedEvent;
import com.vsct.dt.strowgr.admin.core.event.out.DeleteEntryPointEvent;
import com.vsct.dt.strowgr.admin.gui.cli.ConfigurationCommand;
import com.vsct.dt.strowgr.admin.gui.cli.InitializationCommand;
import com.vsct.dt.strowgr.admin.gui.configuration.StrowgrConfiguration;
import com.vsct.dt.strowgr.admin.gui.factory.NSQConsumersFactory;
import com.vsct.dt.strowgr.admin.gui.healthcheck.ConsulHealthcheck;
import com.vsct.dt.strowgr.admin.gui.healthcheck.NsqHealthcheck;
import com.vsct.dt.strowgr.admin.gui.managed.CommitSchedulerManaged;
import com.vsct.dt.strowgr.admin.gui.managed.NSQProducerManaged;
import com.vsct.dt.strowgr.admin.gui.observable.IncomingEvents;
import com.vsct.dt.strowgr.admin.gui.observable.ManagedHaproxy;
import com.vsct.dt.strowgr.admin.gui.resource.api.*;
import com.vsct.dt.strowgr.admin.gui.subscribers.EventBusSubscriber;
import com.vsct.dt.strowgr.admin.nsq.NSQ;
import com.vsct.dt.strowgr.admin.nsq.producer.NSQDispatcher;
import com.vsct.dt.strowgr.admin.nsq.producer.NSQHttpClient;
import com.vsct.dt.strowgr.admin.repository.consul.ConsulRepository;
import com.vsct.dt.strowgr.admin.template.generator.MustacheTemplateGenerator;
import com.vsct.dt.strowgr.admin.template.locator.UriTemplateLocator;
import fr.vsct.dt.nsq.NSQConfig;
import fr.vsct.dt.nsq.NSQProducer;
import fr.vsct.dt.nsq.lookup.NSQLookup;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.UnicastProcessor;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
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
        if (configuration.getNsqChannel() != null) {
            NSQ.CHANNEL = configuration.getNsqChannel();
        }

        /* Main EventBus */
        BlockingQueue eventBusQueue = new ArrayBlockingQueue<>(100);
        ExecutorService executor = environment.lifecycle().executorService("main-bus-handler-threads").workQueue(eventBusQueue).minThreads(configuration.getThreads()).maxThreads(configuration.getThreads()).build();

        EventBus eventBus = new AsyncEventBus(executor, (exception, context) -> {
            LOGGER.error("exception on main event bus. Context: " + subscriberExceptionContextToString(context), exception);
        });
        eventBus.register(this); // for dead events

        /* Templates */
        TemplateGenerator templateGenerator = new MustacheTemplateGenerator();
        UriTemplateLocator templateLocator = new UriTemplateLocator();

        /* Repository */
        ConsulRepository repository = configuration.getConsulRepositoryFactory().buildAndManageBy(environment);
        repository.init();

        FlowableProcessor<CommitRequestedEvent> commitRequestedEventProcessor = UnicastProcessor
                .<CommitRequestedEvent>create()
                .toSerialized();

        EntryPointStateManager entryPointStateManager = new EntryPointStateManager(configuration.getCommitTimeout(), repository);

        /* EntryPoint State Machine */
        EntryPointEventHandler eventHandler = new EntryPointEventHandler(
                entryPointStateManager, repository, repository,
                templateLocator, templateGenerator, eventBus,
                commitRequestedEventProcessor);

        eventBus.register(eventHandler);

        /* NSQ Consumers */
        //Object mapper used for NSQ messages
        ObjectMapper objectMapper = new ObjectMapper();
        // retrieve NSQLookup configuration
        NSQLookup nsqLookup = configuration.getNsqLookupfactory().build();
        //NSQConsumers configuration
        NSQConfig consumerNsqConfig = configuration.getNsqConsumerConfigFactory().build();

        NSQConsumersFactory nsqConsumersFactory = NSQConsumersFactory.make(nsqLookup, consumerNsqConfig, objectMapper);

        ManagedHaproxy managedHaproxy = ManagedHaproxy.create(repository, configuration.getHandledHaproxyRefreshPeriodSecond());
        Observable<ManagedHaproxy.HaproxyAction> hapRegistrationActionsObservable = managedHaproxy.registrationActionsObservable();

        IncomingEvents incomingEvents = IncomingEvents.watch(hapRegistrationActionsObservable, nsqConsumersFactory);

        Observable<EntryPointEvent> nsqEventsObservable = incomingEvents.registerServerEventObservable()
                .map(e -> (EntryPointEvent) e)//Downcast
                .mergeWith(incomingEvents.commitFailureEventObservale())
                .mergeWith(incomingEvents.commitSuccessEventObservale());

        //Push all nsq events to eventBus
        //We observeOn a single thread to avoid blocking nio eventloops
        //NSQToEventBusSubscriber applies backpressure in regard to the eventBusQueue
        nsqEventsObservable.observeOn(Schedulers.newThread()).subscribe(new EventBusSubscriber(eventBus, eventBusQueue));

        /* Manage resources */
        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() throws Exception {
                managedHaproxy.startLookup();
            }

            @Override
            public void stop() throws Exception {
                managedHaproxy.stopLookup();
                incomingEvents.shutdownConsumers();
            }
        });

        /* NSQ Producers */
        NSQProducer nsqProducer = configuration.getNsqProducerFactory().build();

        // manage NSQProducer lifecycle by Dropwizard
        environment.lifecycle().manage(new NSQProducerManaged(nsqProducer));

        ToNSQSubscriber toNSQSubscriber = new ToNSQSubscriber(new NSQDispatcher(nsqProducer));

        commitRequestedEventProcessor
                .observeOn(io.reactivex.schedulers.Schedulers.io())
                .subscribe(toNSQSubscriber::handle);

        /* TryCommitPendingConfiguration */
        FlowableProcessor<TryCommitPendingConfigurationEvent> tryCommitPendingConfigurationProcessor = UnicastProcessor
                .<TryCommitPendingConfigurationEvent>create()
                .toSerialized();

        tryCommitPendingConfigurationProcessor
                .observeOn(io.reactivex.schedulers.Schedulers.io())
                .subscribe(eventHandler::handle);

        /* TryCommitCurrentConfiguration */
        FlowableProcessor<TryCommitCurrentConfigurationEvent> tryCommitCurrentConfigurationProcessor = UnicastProcessor
                .<TryCommitCurrentConfigurationEvent>create()
                .toSerialized();

        tryCommitCurrentConfigurationProcessor
                .observeOn(io.reactivex.schedulers.Schedulers.io())
                .subscribe(eventHandler::handle);

        /* Commit schedulers */
        long periodMilliPendingCurrentScheduler = configuration
                .getPeriodicSchedulerFactory()
                .getPeriodicCommitPendingSchedulerFactory()
                .getPeriodMilli();
        long periodMilliCommitCurrentScheduler = configuration
                .getPeriodicSchedulerFactory()
                .getPeriodicCommitCurrentSchedulerFactory()
                .getPeriodMilli();

        CommitSchedulerManaged<TryCommitPendingConfigurationEvent> commitPendingScheduler = new CommitSchedulerManaged<>("Commit Pending", repository, entryPoint ->
                new TryCommitPendingConfigurationEvent(CorrelationId.newCorrelationId(), new EntryPointKeyDefaultImpl(entryPoint)),
                tryCommitPendingConfigurationProcessor::onNext, periodMilliPendingCurrentScheduler);
        environment.lifecycle().manage(commitPendingScheduler);

        CommitSchedulerManaged<TryCommitCurrentConfigurationEvent> commitCurrentScheduler = new CommitSchedulerManaged<>("Commit Current", repository, ep ->
                new TryCommitCurrentConfigurationEvent(CorrelationId.newCorrelationId(), new EntryPointKeyDefaultImpl(ep)),
                tryCommitCurrentConfigurationProcessor::onNext, periodMilliCommitCurrentScheduler);
        environment.lifecycle().manage(commitCurrentScheduler);

        /* AutoReloadConfig */
        FlowableProcessor<AutoReloadConfigEvent> autoReloadConfigProcessor = UnicastProcessor
                .<AutoReloadConfigEvent>create()
                .toSerialized(); // support Thread-safe onNext calls

        autoReloadConfigProcessor
                .observeOn(io.reactivex.schedulers.Schedulers.io())
                .subscribe(new AutoReloadConfigSubscriber(entryPointStateManager));

        /* AddEntryPoint */
        FlowableProcessor<AddEntryPointEvent> addEntryPointProcessor = UnicastProcessor
                .<AddEntryPointEvent>create()
                .toSerialized(); // support Thread-safe onNext calls

        addEntryPointProcessor
                .observeOn(io.reactivex.schedulers.Schedulers.io())
                .subscribe(new AddEntryPointSubscriber(entryPointStateManager, repository));

        /* UpdateEntryPoint */
        FlowableProcessor<UpdateEntryPointEvent> updateEntryPointProcessor = UnicastProcessor
                .<UpdateEntryPointEvent>create()
                .toSerialized(); // support Thread-safe onNext calls

        updateEntryPointProcessor
                .observeOn(io.reactivex.schedulers.Schedulers.io())
                .subscribe(new UpdateEntryPointSubscriber(entryPointStateManager));

        /* DeleteEntryPoint */
        FlowableProcessor<DeleteEntryPointEvent> deleteEntryPointProcessor = UnicastProcessor
                .<DeleteEntryPointEvent>create()
                .toSerialized();

        deleteEntryPointProcessor
                .observeOn(io.reactivex.schedulers.Schedulers.io())
                .subscribe(toNSQSubscriber::handle);

        /* RegisterServer */
        FlowableProcessor<RegisterServerEvent> registerServerProcessor = UnicastProcessor
                .<RegisterServerEvent>create()
                .toSerialized();

        registerServerProcessor
                .observeOn(io.reactivex.schedulers.Schedulers.io())
                .subscribe(eventHandler::handle);

        /* CommitSuccessEvent */
        FlowableProcessor<CommitSuccessEvent> commitSuccessProcessor = UnicastProcessor
                .<CommitSuccessEvent>create()
                .toSerialized();

        commitSuccessProcessor
                .observeOn(io.reactivex.schedulers.Schedulers.io())
                .subscribe(eventHandler::handle);

        /* CommitFailureEvent */
        FlowableProcessor<CommitFailureEvent> commitFailureProcessor = UnicastProcessor
                .<CommitFailureEvent>create()
                .toSerialized();

        commitFailureProcessor
                .observeOn(io.reactivex.schedulers.Schedulers.io())
                .subscribe(eventHandler::handle);

        /* REST Resources */
        EntryPointResources restApiResource = new EntryPointResources(
                repository,
                autoReloadConfigProcessor, addEntryPointProcessor,
                updateEntryPointProcessor, deleteEntryPointProcessor,
                tryCommitPendingConfigurationProcessor,
                tryCommitCurrentConfigurationProcessor,
                registerServerProcessor,
                commitSuccessProcessor,
                commitFailureProcessor
        );
        environment.jersey().register(restApiResource);

        HaproxyResources haproxyResources = new HaproxyResources(repository, templateLocator, templateGenerator);
        environment.jersey().register(haproxyResources);

        PortResources portResources = new PortResources(repository);
        environment.jersey().register(portResources);

        UriTemplateResources uriTemplateResources = new UriTemplateResources(templateLocator, templateGenerator);
        environment.jersey().register(uriTemplateResources);

        AdminResources adminResources = new AdminResources(nsqLookup);
        environment.jersey().register(adminResources);

        /* Http Client */
        CloseableHttpClient httpClient = new HttpClientBuilder(environment)
                .using(configuration.getHttpClientConfiguration())
                .build("http-client");
        NSQHttpClient nsqdHttpClient = new NSQHttpClient("http://" + configuration.getNsqProducerFactory().getHost() + ":" + configuration.getNsqProducerFactory().getHttpPort(), httpClient);
        NSQHttpClient nsqLookupdHttpClient = new NSQHttpClient("http://" + configuration.getNsqLookupfactory().getHost() + ":" + configuration.getNsqLookupfactory().getPort(), httpClient);

        /* Healthchecks */
        environment.healthChecks().register("version", new NsqHealthcheck(nsqLookupdHttpClient));
        environment.healthChecks().register("nsqproducer", new NsqHealthcheck(nsqdHttpClient));
        environment.healthChecks().register("consul", new ConsulHealthcheck(configuration.getConsulRepositoryFactory().getHost(), configuration.getConsulRepositoryFactory().getPort()));

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
