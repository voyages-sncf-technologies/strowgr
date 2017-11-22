/*
 * Copyright (C) 2016 VSCT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vsct.dt.strowgr.admin.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsct.dt.strowgr.admin.core.*;
import com.vsct.dt.strowgr.admin.core.entrypoint.*;
import com.vsct.dt.strowgr.admin.core.event.CorrelationId;
import com.vsct.dt.strowgr.admin.core.event.in.*;
import com.vsct.dt.strowgr.admin.core.event.out.CommitRequestedEvent;
import com.vsct.dt.strowgr.admin.core.event.out.DeleteEntryPointEvent;
import com.vsct.dt.strowgr.admin.core.repository.EntryPointRepository;
import com.vsct.dt.strowgr.admin.core.security.model.User;
import com.vsct.dt.strowgr.admin.gui.cli.ConfigurationCommand;
import com.vsct.dt.strowgr.admin.gui.cli.InitializationCommand;
import com.vsct.dt.strowgr.admin.gui.configuration.StrowgrConfiguration;
import com.vsct.dt.strowgr.admin.gui.factory.NSQConsumersFactory;
import com.vsct.dt.strowgr.admin.gui.healthcheck.ConsulHealthcheck;
import com.vsct.dt.strowgr.admin.gui.healthcheck.NsqHealthcheck;
import com.vsct.dt.strowgr.admin.gui.observable.EntryPointPublisher;
import com.vsct.dt.strowgr.admin.gui.managed.ManagedNSQConsumer;
import com.vsct.dt.strowgr.admin.gui.managed.ManagedScheduledFlowable;
import com.vsct.dt.strowgr.admin.gui.managed.ManagedNSQProducer;
import com.vsct.dt.strowgr.admin.gui.observable.HAProxyPublisher;
import com.vsct.dt.strowgr.admin.gui.observable.HAProxySubscriber;
import com.vsct.dt.strowgr.admin.gui.resource.api.*;
import com.vsct.dt.strowgr.admin.gui.security.CorrectedCachingAuthenticator;
import com.vsct.dt.strowgr.admin.gui.security.NoAuthValueFactoryProvider;
import com.vsct.dt.strowgr.admin.nsq.NSQ;
import com.vsct.dt.strowgr.admin.nsq.consumer.FlowableNSQConsumer;
import com.vsct.dt.strowgr.admin.nsq.producer.CommitRequestedSubscriber;
import com.vsct.dt.strowgr.admin.nsq.producer.DeleteEntryPointSubscriber;
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
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.reactivex.Flowable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.processors.UnicastProcessor;
import io.reactivex.schedulers.Schedulers;

import org.apache.http.impl.client.CloseableHttpClient;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import java.util.concurrent.TimeUnit;

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

    @Override
    public void run(StrowgrConfiguration configuration, Environment environment) throws Exception {
        LOGGER.info("start dropwizard configuration");

        /* Allow the use of another NSQ channel for development purposes */
        if (configuration.getNsqChannel() != null) {
            NSQ.CHANNEL = configuration.getNsqChannel();
        }

        /* Templates */
        TemplateGenerator templateGenerator = new MustacheTemplateGenerator();
        UriTemplateLocator templateLocator = new UriTemplateLocator();

        /* Repository */
        ConsulRepository repository = configuration.getConsulRepositoryFactory().buildAndManageBy(environment);
        repository.init();

        // Object mapper used for NSQ messages
        ObjectMapper objectMapper = new ObjectMapper();

        /* NSQ Consumers */
        NSQLookup nsqLookup = configuration.getNsqLookupfactory().build();
        NSQConfig nsqConfig = configuration.getNsqConsumerConfigFactory().build();
        NSQConsumersFactory nsqConsumersFactory = new NSQConsumersFactory(nsqLookup, nsqConfig, objectMapper);

        /* NSQ Producers */
        NSQProducer nsqProducer = configuration.getNsqProducerFactory().build();
        environment.lifecycle().manage(new ManagedNSQProducer(nsqProducer));
        NSQDispatcher nsqDispatcher = new NSQDispatcher(nsqProducer);

        Subscriber<CommitRequestedEvent> commitRequestedEventProcessor = commitRequestedSubscriber(nsqDispatcher);

        /* EntryPoint State Machine */
        EntryPointStateManager entryPointStateManager = new EntryPointStateManager(configuration.getCommitTimeout(), repository);

        EntryPointEventHandler eventHandler = new EntryPointEventHandler(
                entryPointStateManager, repository, repository,
                templateLocator, templateGenerator,
                commitRequestedEventProcessor);

        /* Publishers */
        Publisher<HAProxyPublisher.HAProxyAction> haProxyActionProcessor = haProxyActionPublisher(configuration, environment, repository);

        /* Subscribers */
        Subscriber<AutoReloadConfigEvent> autoReloadConfigProcessor = autoReloadConfigSubscriber(entryPointStateManager);

        Subscriber<AddEntryPointEvent> addEntryPointProcessor = addEntryPointSubscriber(repository, entryPointStateManager);

        Subscriber<UpdateEntryPointEvent> updateEntryPointProcessor = updateEntryPointEventSubscriber(entryPointStateManager);

        Subscriber<DeleteEntryPointEvent> deleteEntryPointProcessor = deleteEntryPointSubscriber(nsqDispatcher);

        Subscriber<RegisterServerEvent> registerServerSubscriber = registerServerSubscriber(environment, nsqConsumersFactory, eventHandler);

        Subscriber<CommitCompletedEvent> commitCompletedSubscriber = commitCompletedSubscriber(environment, nsqConsumersFactory, haProxyActionProcessor, eventHandler);

        Subscriber<CommitFailedEvent> commitFailedSubscriber = commitFailedSubscriber(environment, nsqConsumersFactory, haProxyActionProcessor, eventHandler);

        Subscriber<TryCommitPendingConfigurationEvent> tryCommitPendingSubscriber = tryCommitPendingConfigurationSubscriber(configuration, environment, repository, eventHandler);

        Subscriber<TryCommitCurrentConfigurationEvent> tryCommitCurrentSubscriber = tryCommitCurrentConfigurationSubscriber(configuration, environment, repository, eventHandler);

        /* REST Resources */
        EntryPointResources restApiResource = new EntryPointResources(
                repository,
                autoReloadConfigProcessor,
                addEntryPointProcessor,
                updateEntryPointProcessor,
                deleteEntryPointProcessor,
                tryCommitPendingSubscriber,
                tryCommitCurrentSubscriber,
                registerServerSubscriber,
                commitCompletedSubscriber,
                commitFailedSubscriber
        );
        environment.jersey().register(restApiResource);

        HaproxyResources haproxyResources = new HaproxyResources(repository, templateLocator, templateGenerator);
        environment.jersey().register(haproxyResources);

        PortResources portResources = new PortResources(repository);
        environment.jersey().register(portResources);

        AggregateProxyResources proxyResources = new AggregateProxyResources(restApiResource, haproxyResources);
        environment.jersey().register(proxyResources);

        
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
        environment.jersey().register(new IncompleteConfigurationExceptionMapper());
        
        if (configuration.getAuthenticator().isPresent()) {
        
        	CorrectedCachingAuthenticator cachingAuthenticator	=	new CorrectedCachingAuthenticator<>(environment.metrics(), configuration.getAuthenticator().get(), configuration.getAuthenticationCachePolicy());
        
	        environment.jersey().register(new AuthDynamicFeature(new BasicCredentialAuthFilter.Builder<User>()
	                .setAuthenticator(cachingAuthenticator)
	                //.setAuthorizer(new LDAPAuthorizerMock())
	                .setRealm("STROWGR LDAP")
	                .buildAuthFilter()));
	        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(User.class));
        } else {
        	environment.jersey().register(new NoAuthValueFactoryProvider.Binder<>(User.class));

        }
    	environment.jersey().register(RolesAllowedDynamicFeature.class);
    }

    private Publisher<HAProxyPublisher.HAProxyAction> haProxyActionPublisher(StrowgrConfiguration configuration, Environment environment, ConsulRepository repository) {

        /* HAPRoxyPublisher: scheduled lookup */
        ManagedScheduledFlowable haProxyFlowable = new ManagedScheduledFlowable("HA Proxy", configuration.getHandledHaproxyRefreshPeriodSecond(), TimeUnit.SECONDS, Schedulers.newThread());
        environment.lifecycle().manage(haProxyFlowable);

        Flowable<HAProxyPublisher.HAProxyAction> haProxyActionFlowable = haProxyFlowable.getFlowable()
                .flatMap(new HAProxyPublisher(repository));

        /* using intermediate processor otherwise HAProxyPublisher will be called by every subscriber */
        PublishProcessor<HAProxyPublisher.HAProxyAction> haProxyActionProcessor = PublishProcessor.create();
        haProxyActionFlowable.subscribe(haProxyActionProcessor);

        return haProxyActionProcessor;
    }

    private Subscriber<DeleteEntryPointEvent> deleteEntryPointSubscriber(NSQDispatcher nsqDispatcher) {

        FlowableProcessor<DeleteEntryPointEvent> deleteEntryPointProcessor = UnicastProcessor
                .<DeleteEntryPointEvent>create()
                .toSerialized();

        deleteEntryPointProcessor
                .observeOn(Schedulers.io())
                .subscribe(new DeleteEntryPointSubscriber(nsqDispatcher));

        return deleteEntryPointProcessor;
    }

    private Subscriber<UpdateEntryPointEvent> updateEntryPointEventSubscriber(EntryPointStateManager entryPointStateManager) {

        FlowableProcessor<UpdateEntryPointEvent> updateEntryPointProcessor = UnicastProcessor
                .<UpdateEntryPointEvent>create()
                .toSerialized();

        updateEntryPointProcessor
                .observeOn(Schedulers.io())
                .subscribe(new UpdateEntryPointSubscriber(entryPointStateManager));

        return updateEntryPointProcessor;
    }

    private Subscriber<AddEntryPointEvent> addEntryPointSubscriber(ConsulRepository repository, EntryPointStateManager entryPointStateManager) {

        FlowableProcessor<AddEntryPointEvent> addEntryPointProcessor = UnicastProcessor
                .<AddEntryPointEvent>create()
                .toSerialized();

        addEntryPointProcessor
                .observeOn(Schedulers.io())
                .subscribe(new AddEntryPointSubscriber(entryPointStateManager, repository));

        return addEntryPointProcessor;
    }

    private Subscriber<AutoReloadConfigEvent> autoReloadConfigSubscriber(EntryPointStateManager entryPointStateManager) {

        FlowableProcessor<AutoReloadConfigEvent> autoReloadConfigProcessor = UnicastProcessor
                .<AutoReloadConfigEvent>create()
                .toSerialized();

        autoReloadConfigProcessor
                .observeOn(Schedulers.io())
                .subscribe(new AutoReloadConfigSubscriber(entryPointStateManager));

        return autoReloadConfigProcessor;
    }

    private Subscriber<CommitRequestedEvent> commitRequestedSubscriber(NSQDispatcher nsqDispatcher) {

        FlowableProcessor<CommitRequestedEvent> commitRequestedEventProcessor = UnicastProcessor
                .<CommitRequestedEvent>create()
                .toSerialized();

        commitRequestedEventProcessor
                .observeOn(Schedulers.io())
                .subscribe(new CommitRequestedSubscriber(nsqDispatcher));

        return commitRequestedEventProcessor;
    }

    private Subscriber<RegisterServerEvent> registerServerSubscriber(Environment environment, NSQConsumersFactory nsqConsumersFactory, EntryPointEventHandler eventHandler) {

        FlowableNSQConsumer<RegisterServerEvent> registerServerConsumer = nsqConsumersFactory.buildRegisterServerConsumer();
        environment.lifecycle().manage(new ManagedNSQConsumer(registerServerConsumer));

        FlowableProcessor<RegisterServerEvent> registerServerProcessor = UnicastProcessor
                .<RegisterServerEvent>create()
                .toSerialized();

        registerServerProcessor
                .mergeWith(registerServerConsumer.flowable())
                .observeOn(Schedulers.io())
                .subscribe(eventHandler::handle);

        return registerServerProcessor;
    }

    private Subscriber<CommitFailedEvent> commitFailedSubscriber(Environment environment, NSQConsumersFactory nsqConsumersFactory, Publisher<HAProxyPublisher.HAProxyAction> haProxyActionProcessor, EntryPointEventHandler eventHandler) {

        FlowableProcessor<CommitFailedEvent> commitFailedEventProcessor = UnicastProcessor.<CommitFailedEvent>create().toSerialized();

        /* HAProxySubscriber: Creates a dedicated NSQConsumer for each HAProxy CommitFailed topic */
        HAProxySubscriber<CommitFailedEvent> commitFailedHAProxySubscriber = new HAProxySubscriber<>(nsqConsumersFactory::buildCommitFailedConsumer, commitFailedEventProcessor);
        haProxyActionProcessor.subscribe(commitFailedHAProxySubscriber);
        environment.lifecycle().manage(commitFailedHAProxySubscriber);

        FlowableProcessor<CommitFailedEvent> commitFailedProcessor = UnicastProcessor
                .<CommitFailedEvent>create()
                .toSerialized();

        commitFailedProcessor
                .mergeWith(commitFailedEventProcessor)
                .observeOn(Schedulers.io())
                .subscribe(eventHandler::handle);

        return commitFailedProcessor;
    }

    private Subscriber<CommitCompletedEvent> commitCompletedSubscriber(Environment environment, NSQConsumersFactory nsqConsumersFactory, Publisher<HAProxyPublisher.HAProxyAction> haProxyActionProcessor, EntryPointEventHandler eventHandler) {

        FlowableProcessor<CommitCompletedEvent> commitCompletedEventProcessor = UnicastProcessor.<CommitCompletedEvent>create().toSerialized();

        /* HAProxySubscriber: Creates a dedicated NSQConsumer for each HAProxy CommitCompleted topic */
        HAProxySubscriber<CommitCompletedEvent> commitCompletedHAProxySubscriber = new HAProxySubscriber<>(nsqConsumersFactory::buildCommitCompletedConsumer, commitCompletedEventProcessor);
        haProxyActionProcessor.subscribe(commitCompletedHAProxySubscriber);
        environment.lifecycle().manage(commitCompletedHAProxySubscriber);

        FlowableProcessor<CommitCompletedEvent> commitCompletedProcessor = UnicastProcessor
                .<CommitCompletedEvent>create()
                .toSerialized();

        commitCompletedProcessor
                .mergeWith(commitCompletedEventProcessor)
                .observeOn(Schedulers.io())
                .subscribe(eventHandler::handle);

        return commitCompletedProcessor;
    }

    private Subscriber<TryCommitPendingConfigurationEvent> tryCommitPendingConfigurationSubscriber(
            StrowgrConfiguration configuration, Environment environment,
            EntryPointRepository repository, EntryPointEventHandler eventHandler) {

        long periodMilliPendingCurrentScheduler = configuration
                .getPeriodicSchedulerFactory()
                .getPeriodicCommitPendingSchedulerFactory()
                .getPeriodMilli();

        FlowableProcessor<TryCommitPendingConfigurationEvent> tryCommitPendingConfigurationProcessor = UnicastProcessor
                .<TryCommitPendingConfigurationEvent>create()
                .toSerialized();

        tryCommitPendingConfigurationProcessor
                .observeOn(Schedulers.io())
                .subscribe(eventHandler::handle);

        ManagedScheduledFlowable commitPendingFlowable = new ManagedScheduledFlowable("Commit Pending", periodMilliPendingCurrentScheduler, TimeUnit.MILLISECONDS, Schedulers.newThread());
        environment.lifecycle().manage(commitPendingFlowable);

        commitPendingFlowable.getFlowable()
                .flatMap(new EntryPointPublisher<>(repository, entryPoint -> new TryCommitPendingConfigurationEvent(CorrelationId.newCorrelationId(), new EntryPointKeyDefaultImpl(entryPoint))))
                .subscribe(tryCommitPendingConfigurationProcessor);

        return tryCommitPendingConfigurationProcessor;
    }

    private Subscriber<TryCommitCurrentConfigurationEvent> tryCommitCurrentConfigurationSubscriber(
            StrowgrConfiguration configuration, Environment environment,
            EntryPointRepository repository, EntryPointEventHandler eventHandler) {

        long periodMilliCommitCurrentScheduler = configuration
                .getPeriodicSchedulerFactory()
                .getPeriodicCommitCurrentSchedulerFactory()
                .getPeriodMilli();

        FlowableProcessor<TryCommitCurrentConfigurationEvent> tryCommitCurrentConfigurationProcessor = UnicastProcessor
                .<TryCommitCurrentConfigurationEvent>create()
                .toSerialized();

        tryCommitCurrentConfigurationProcessor
                .observeOn(Schedulers.io())
                .subscribe(eventHandler::handle);

        ManagedScheduledFlowable commitCurrentFlowable = new ManagedScheduledFlowable("Commit Current", periodMilliCommitCurrentScheduler, TimeUnit.MILLISECONDS, Schedulers.newThread());
        environment.lifecycle().manage(commitCurrentFlowable);

        commitCurrentFlowable.getFlowable()
                .flatMap(new EntryPointPublisher<>(repository, entryPoint -> new TryCommitCurrentConfigurationEvent(CorrelationId.newCorrelationId(), new EntryPointKeyDefaultImpl(entryPoint))))
                .subscribe(tryCommitCurrentConfigurationProcessor);

        return tryCommitCurrentConfigurationProcessor;
    }

    private static class IncompleteConfigurationExceptionMapper implements ExceptionMapper<IncompleteConfigurationException> {
        @Override
        public Response toResponse(IncompleteConfigurationException e) {
            return Response.status(500).entity(e.getMessage()).type(MediaType.TEXT_PLAIN_TYPE).build();
        }
    }
}
