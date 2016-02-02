package com.vsct.dt.haas.dropwizard;


import com.codahale.metrics.MetricRegistry;
import com.vsct.dt.haas.dropwizard.resources.RestApiResources;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public void run(HaasConfiguration haasConfiguration, Environment environment) throws Exception{
        MetricRegistry metricRegistry = environment.metrics();


        //CatalogService catalogService = new CatalogService(new File(config.getCatalogFile()));
        RestApiResources restApiResource = new RestApiResources();

        environment.jersey().register(restApiResource);

   //     environment.healthChecks().register("ping", new PingHealthCheck());
    }

}
