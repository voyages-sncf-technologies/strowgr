package com.vsct.dt.haas.admin.gui;

import com.github.brainlag.nsq.lookup.NSQLookup;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.vsct.dt.haas.admin.core.EntryPointEventHandler;
import com.vsct.dt.haas.admin.core.TemplateGenerator;
import com.vsct.dt.haas.admin.gui.configuration.HaasConfiguration;
import com.vsct.dt.haas.admin.gui.healthcheck.ConsulHealthcheck;
import com.vsct.dt.haas.admin.gui.healthcheck.NsqHealthcheck;
import com.vsct.dt.haas.admin.gui.resource.api.HaproxyResources;
import com.vsct.dt.haas.admin.gui.resource.api.EntrypointResources;
import com.vsct.dt.haas.admin.gui.resource.api.PortResources;
import com.vsct.dt.haas.admin.nsq.producer.Producer;
import com.vsct.dt.haas.admin.repository.consul.ConsulRepository;
import com.vsct.dt.haas.admin.template.generator.MustacheTemplateGenerator;
import com.vsct.dt.haas.admin.template.locator.UriTemplateLocator;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

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
        LOGGER.info("start dropwizard configuration");

        /* Main EventBus */
        ExecutorService executor = environment.lifecycle().executorService("main-bus-handler-threads").minThreads(configuration.getThreads()).maxThreads(configuration.getThreads()).build();
        EventBus eventBus = new AsyncEventBus(executor);

        /* Templates */
        TemplateGenerator templateGenerator = new MustacheTemplateGenerator();
        UriTemplateLocator templateLocator = new UriTemplateLocator();

        /* Repository */
        ConsulRepository repository = configuration.getConsulRepositoryFactory().build(environment);

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
        NSQLookup lookup = configuration.getNsqLookupfactory().build(environment);
        configuration.getCommitMessageConsumerFactory().build(lookup, configuration.getDefaultHAPName(), eventBus::post, environment);
        configuration.getRegisterServerMessageConsumerFactory().build(lookup, eventBus::post, environment);

        /* NSQ Producers */
        Producer producer = configuration.getNsqProducerFactory().build(environment);

        CommitBeginEventListener commitBeginEventListener = new CommitBeginEventListener(producer);
        eventBus.register(commitBeginEventListener);

        /* Commit schedulers */
        configuration.getPeriodicSchedulerFactory().getPeriodicCommitCurrentSchedulerFactory().build(repository, eventBus::post, environment);
        configuration.getPeriodicSchedulerFactory().getPeriodicCommitPendingSchedulerFactory().build(repository, eventBus::post, environment);

        /* REST Resources */
        EntrypointResources restApiResource = new EntrypointResources(eventBus, repository, repository, templateLocator, templateGenerator);
        environment.jersey().register(restApiResource);

        HaproxyResources haproxyResources = new HaproxyResources(repository);
        environment.jersey().register(haproxyResources);

        PortResources portResources = new PortResources(repository);
        environment.jersey().register(portResources);

        eventBus.register(restApiResource);

        /* Healthchecks */
        environment.healthChecks().register("nsqlookup", new NsqHealthcheck(configuration.getNsqLookupfactory().getHost(), configuration.getNsqLookupfactory().getPort()));
        // the healthcheck on producer is done on http port which is by convention tcp port + 1
        environment.healthChecks().register("nsqproducer", new NsqHealthcheck(configuration.getNsqProducerFactory().getHost(), configuration.getNsqProducerFactory().getPort() + 1));
        environment.healthChecks().register("consul", new ConsulHealthcheck(configuration.getConsulRepositoryFactory().getHost(), configuration.getConsulRepositoryFactory().getPort()));
    }

}
