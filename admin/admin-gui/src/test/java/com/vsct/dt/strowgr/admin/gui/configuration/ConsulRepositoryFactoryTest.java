package com.vsct.dt.strowgr.admin.gui.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsct.dt.strowgr.admin.gui.configuration.scheduler.PeriodicSchedulerFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.logging.DefaultLoggingFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.thread.Scheduler;
import org.glassfish.jersey.client.internal.HttpUrlConnector;
import org.junit.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * Created by Guillaume_Arnaud on 20/01/2017.
 */
public class ConsulRepositoryFactoryTest {

    @Test
    public void should_generate_configuration() throws JsonProcessingException {
        System.out.println(new ObjectMapper().writeValueAsString(new ConsulRepositoryFactory()));
        HashMap<String, Object> stringObjectHashMap = new HashMap<>();

        stringObjectHashMap.put("repository", new ConsulRepositoryFactory());
        stringObjectHashMap.put("nsqLookup", new NSQLookupFactory());
        stringObjectHashMap.put("nsqProducer", new NSQProducerFactory());
        stringObjectHashMap.put("nsqProducer", new NSQProducerFactory());

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);

        StrowgrConfiguration configuration = new StrowgrConfiguration();
        configuration.setConsulRepositoryFactory(new ConsulRepositoryFactory());
        configuration.setNsqLookupfactory(new NSQLookupFactory());
        configuration.setNsqProducerConfigFactory(new NSQConfigFactory());
        configuration.setPeriodicSchedulerFactory(new PeriodicSchedulerFactory());
        configuration.setLoggingFactory(new DefaultLoggingFactory());
        configuration.setThreads(200);
        configuration.setCommitTimeout(13);
        configuration.setHandledHaproxyRefreshPeriodSecond(20);
        configuration.setServerFactory(new ServerFactory() {
            public String type = "simple";
            public String rootPath = "/api";
            public String applicationContextPath = "/";
            public HttpConnectorFactory applicationConnector = new HttpConnectorFactory();

            {
                applicationConnector.setPort(8080);
                applicationConnector.setBindHost("localhost");
            }

            @Override
            public Server build(Environment environment) {
                return null;
            }
        });
        System.out.println(yaml.dump(configuration));
    }

}