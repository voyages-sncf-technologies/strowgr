package com.vsct.dt.haas.admin.gui;

import com.codahale.metrics.MetricRegistry;
import com.github.brainlag.nsq.lookup.DefaultNSQLookup;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vsct.dt.haas.admin.core.EntryPointEventHandler;
import com.vsct.dt.haas.admin.core.EntryPointRepository;
import com.vsct.dt.haas.admin.core.TemplateGenerator;
import com.vsct.dt.haas.admin.core.TemplateLocator;
import com.vsct.dt.haas.admin.core.event.out.CommitBeginEvent;
import com.vsct.dt.haas.admin.gui.resource.RestApiResources;
import com.vsct.dt.haas.admin.nsq.consumer.CommitMessageConsumer;
import com.vsct.dt.haas.admin.nsq.consumer.RegisterServerMessageConsumer;
import com.vsct.dt.haas.admin.nsq.producer.Producer;
import com.vsct.dt.haas.admin.repository.consul.ConsulRepository;
import com.vsct.dt.haas.admin.scheduler.RecurrentScheduler;
import com.vsct.dt.haas.admin.template.generator.MustacheTemplateGenerator;
import com.vsct.dt.haas.admin.template.locator.UriTemplateLocator;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HaasMain extends Application<HaasConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HaasMain.class);

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
    public void run(HaasConfiguration configuration, Environment environment) throws Exception {
        MetricRegistry metricRegistry = environment.metrics();

        /* Main EventBus */
        ExecutorService executor = environment.lifecycle().executorService("main-bus-handler-threads").minThreads(50).maxThreads(50).build();
        EventBus eventBus = new AsyncEventBus(executor);

        /* Templates */
        TemplateGenerator templateGenerator = new MustacheTemplateGenerator();
        TemplateLocator templateLocator = new UriTemplateLocator();

        /* Repository */
        ConsulRepository repository = configuration.getConsulRepositoryFactory().build(environment);

        /* EntryPoint State Machine */
        EntryPointEventHandler eventHandler = EntryPointEventHandler
                .backedBy(repository)
                .findTemplatesWith(templateLocator)
                .generatesTemplatesWith(templateGenerator)
                .outputMessagesTo(eventBus);

        eventBus.register(eventHandler);

        /* NSQ Consumers */
        NSQLookup lookup = new DefaultNSQLookup();
        lookup.addLookupAddress("localhost", 50161);

        CommitMessageConsumer commitMessageConsumer = new CommitMessageConsumer(lookup, "haproxy", eventBus::post);
        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() throws Exception {
                LOGGER.info("Starting CommitMessageConsumer");
                commitMessageConsumer.start();
            }

            @Override
            public void stop() throws Exception {
                LOGGER.info("Stopping CommitMessageConsumer");
                commitMessageConsumer.stop();
            }
        });


        RegisterServerMessageConsumer registerServerMessageConsumer = new RegisterServerMessageConsumer(lookup, eventBus::post);
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

        /* NSQ Producers */
        Producer producer = new Producer("localhost", 50161);
        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() throws Exception {
                LOGGER.info("Starting NSQProducer");
                registerServerMessageConsumer.start();
            }

            @Override
            public void stop() throws Exception {
                LOGGER.info("Stopping NSQProducer");
                registerServerMessageConsumer.stop();
            }
        });

        CommitBeginEventListener commitBeginEventListener = new CommitBeginEventListener(producer);
        eventBus.register(commitBeginEventListener);

        /* Commit schedulers */
        RecurrentScheduler commitCurrentScheduler = RecurrentScheduler.newRecurrentCommitCurrentScheduler(repository, eventBus::post, 10000);
        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() throws Exception {
                LOGGER.info("Starting CommitCurrentScheduler");
                commitCurrentScheduler.start();
            }

            @Override
            public void stop() throws Exception {
                LOGGER.info("Stopping CommitCurrentScheduler");
                commitCurrentScheduler.stop();
            }
        });

        RecurrentScheduler commitPendingScheduler = RecurrentScheduler.newRecurrentCommitPendingScheduler(repository, eventBus::post, 10000);
        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() throws Exception {
                LOGGER.info("Starting CommitPendingScheduler");
                commitPendingScheduler.start();
            }

            @Override
            public void stop() throws Exception {
                LOGGER.info("Stopping CommitPendingScheduler");
                commitPendingScheduler.stop();
            }
        });

        /* REST Resource */
        RestApiResources restApiResource = new RestApiResources(eventBus, repository);
        environment.jersey().register(restApiResource);

        eventBus.register(restApiResource);
    }

}
