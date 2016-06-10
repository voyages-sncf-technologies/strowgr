package com.vsct.dt.strowgr.admin.gui;

import com.github.brainlag.nsq.NSQConsumer;
import com.github.brainlag.nsq.NSQProducer;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vsct.dt.strowgr.admin.core.EntryPointEventHandler;
import com.vsct.dt.strowgr.admin.core.TemplateGenerator;
import com.vsct.dt.strowgr.admin.gui.cli.InitializationCommand;
import com.vsct.dt.strowgr.admin.gui.configuration.StrowgrConfiguration;
import com.vsct.dt.strowgr.admin.gui.healthcheck.ConsulHealthcheck;
import com.vsct.dt.strowgr.admin.gui.healthcheck.NsqHealthcheck;
import com.vsct.dt.strowgr.admin.gui.manager.NSQConsumerManager;
import com.vsct.dt.strowgr.admin.gui.manager.NSQProducerManager;
import com.vsct.dt.strowgr.admin.gui.resource.api.EntrypointResources;
import com.vsct.dt.strowgr.admin.gui.resource.api.HaproxyResources;
import com.vsct.dt.strowgr.admin.gui.resource.api.PortResources;
import com.vsct.dt.strowgr.admin.gui.tasks.HaproxyVipTask;
import com.vsct.dt.strowgr.admin.gui.tasks.InitPortsTask;
import com.vsct.dt.strowgr.admin.nsq.producer.NSQDispatcher;
import com.vsct.dt.strowgr.admin.nsq.producer.NSQHttpClient;
import com.vsct.dt.strowgr.admin.repository.consul.ConsulRepository;
import com.vsct.dt.strowgr.admin.template.IncompleteConfigurationException;
import com.vsct.dt.strowgr.admin.template.generator.MustacheTemplateGenerator;
import com.vsct.dt.strowgr.admin.template.locator.UriTemplateLocator;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;

public class StrowgrMain extends Application<StrowgrConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StrowgrMain.class);

    private StrowgrConfiguration strowgrConfiguration = null;

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
        System.out.println("bootstrap");
    }

    @Subscribe
    public void handleDeadEvent(DeadEvent deadEvent) {
        LOGGER.error("an event has no subscribers: {}", deadEvent);
    }

    @Override
    public void run(StrowgrConfiguration configuration, Environment environment) throws Exception {
        System.out.println("run");
        LOGGER.info("start dropwizard configuration");

        /* Main EventBus */
        ExecutorService executor = environment.lifecycle().executorService("main-bus-handler-threads").workQueue(new ArrayBlockingQueue<>(100)).minThreads(configuration.getThreads()).maxThreads(configuration.getThreads()).build();
        EventBus eventBus = new AsyncEventBus(executor, (exception, context) -> {
            LOGGER.error("exception on main event bus. Context: " + context, exception);
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
        // retrieve NSQLookup configuration
        NSQLookup nsqLookup = configuration.getNsqLookupfactory().build();
        // initialize NSQConsumer for commit_completed topic which forwards to EventBus
        NSQConsumer nsqConsumerCommitCompleted = configuration.getCommitCompletedConsumerFactory().build(nsqLookup, configuration.getDefaultHAPName(), eventBus::post);
        // initialize NSQConsumer for commit_failed topic which forwards to EventBus
        NSQConsumer nsqConsumerCommitFailed = configuration.getCommitFailedConsumerFactory().build(nsqLookup, configuration.getDefaultHAPName(), eventBus::post);
        // initialize NSQConsumer for register_server topic which forwards to EventBus
        NSQConsumer nsqConsumerRegisterServer = configuration.getRegisterServerMessageConsumerFactory().build(nsqLookup, eventBus::post);

        // register NSQ consumers to lifecycle
        // TODO use a managed ServiceExecutor (environment.lifecycle().executorService()) and share it with all NSQConsumer ({@code NSQConsumer#setExecutor}) ?
        environment.lifecycle().manage(new NSQConsumerManager(nsqConsumerCommitCompleted));
        environment.lifecycle().manage(new NSQConsumerManager(nsqConsumerCommitFailed));
        environment.lifecycle().manage(new NSQConsumerManager(nsqConsumerRegisterServer));

        /* NSQ Producers */
        NSQProducer nsqProducer = configuration.getNsqProducerFactory().build();
        // manage NSQProducer lifecycle by Dropwizard
        environment.lifecycle().manage(new NSQProducerManager(nsqProducer));
        // Pipeline from eventbus to NSQ producer
        eventBus.register(new ToNSQSubscriber(new NSQDispatcher(nsqProducer)));

        /* Http Client */
        final CloseableHttpClient httpClient = new HttpClientBuilder(environment)
                .using(configuration.getHttpClientConfiguration())
                .build("http-client");
        NSQHttpClient nsqdHttpClient = new NSQHttpClient("http://" + configuration.getNsqProducerFactory().getHost() + ":" + configuration.getNsqProducerFactory().getHttpPort(), httpClient);
        NSQHttpClient nsqLookupdHttpClient = new NSQHttpClient("http://" + configuration.getNsqLookupfactory().getHost() + ":" + configuration.getNsqLookupfactory().getPort(), httpClient);

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

}
