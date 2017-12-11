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
package com.vsct.dt.strowgr.admin.gui.cli;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.vsct.dt.strowgr.admin.gui.configuration.*;
import com.vsct.dt.strowgr.admin.gui.configuration.scheduler.PeriodicCommitCurrentSchedulerFactory;
import com.vsct.dt.strowgr.admin.gui.configuration.scheduler.PeriodicCommitPendingSchedulerFactory;
import com.vsct.dt.strowgr.admin.gui.configuration.scheduler.PeriodicSchedulerFactory;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationCommand extends Command {
    public ConfigurationCommand() {
        super("config", "generate configuration file");
    }


    @Override
    public void configure(Subparser subparser) {
        subparser.addArgument("-o", "--output-file")
                .dest("output-file")
                .type(String.class)
                .required(true)
                .help("output file of generated configuration");

    }

    String generateConfiguration() throws JsonProcessingException {
        // initialize configuration
        StrowgrConfiguration configuration = new StrowgrConfiguration();
        configuration.setConsulRepositoryFactory(new ConsulRepositoryFactory());
        configuration.setNsqLookupfactory(new NSQLookupFactory());
        configuration.setNsqProducerFactory(new NSQProducerFactory());
        configuration.setNsqProducerConfigFactory(new NSQConfigFactory());
        PeriodicSchedulerFactory periodicScheduler = new PeriodicSchedulerFactory();
        periodicScheduler.setPeriodicCommitCurrentSchedulerFactory(new PeriodicCommitCurrentSchedulerFactory());
        periodicScheduler.setPeriodicCommitPendingSchedulerFactory(new PeriodicCommitPendingSchedulerFactory());
        configuration.setPeriodicSchedulerFactory(periodicScheduler);
        configuration.setNsqConsumerConfigFactory(new NSQConfigFactory());
        configuration.setNsqProducerConfigFactory(new NSQConfigFactory());

        // initialize jackson for yaml
        YAMLFactory yamlFactory = new YAMLFactory();
        yamlFactory.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, false);
        ObjectMapper objectMapper = new ObjectMapper(yamlFactory);

        // convert to map for filtering some internal configuration
        Map<String, Object> map = objectMapper.convertValue(configuration, Map.class);
        map.remove("metricsFactory");
        map.remove("metrics");
        map.remove("httpClient");
        
        buildRoot(map);
        
        HashMap<String, Object> loggingValue = buildLoggingSnippet();

        // add custom dropwizard logging snippet
        map.put("logging", loggingValue);
        HashMap<String, Object> server = buildServerSnippet();

        HashMap<String, Object> ldapConfiguration = buildLdapConfiguration();
        map.put("ldapConfiguration", ldapConfiguration);
        
        
        // add custom dropwizard server snippet
        map.put("server", server);
        map.remove("nsqProducerConfigFactory");
        map.remove("nsqConsumerConfigFactory");

        return objectMapper.writeValueAsString(map);
    }

    private void buildRoot( Map<String, Object> map) {
        map.put("authenticatorType", "none");
        map.put("authenticationCachePolicy", "maximumSize=10000, expireAfterAccess=10m");
        map.put("useDefaultUserWhenAuthentFails", "false");
    }

    private HashMap<String, Object> buildLdapConfiguration() {
        // server
        HashMap<String, Object> ldapConfiguration = new HashMap<>();
        ldapConfiguration.put("uri", "TODO");
        ldapConfiguration.put("adDomain", "TODO");
        ldapConfiguration.put("connectTimeout", "1000ms");
        ldapConfiguration.put("readTimeout", "1000ms");
        ldapConfiguration.put("userNameAttribute", "sAMAccountName");
        ldapConfiguration.put("userSearchBase", "dc=mother,dc=com");
        ldapConfiguration.put("roleSearchBase", "ou=kikoo,dc=mother,dc=com");
        ldapConfiguration.put("prodGroupName", "HESPERIDES_PROD_GROUP");
        ldapConfiguration.put("techGroupName", "HESPERIDES_TECH_GROUP");
        
        //// connector
        HashMap<String, String> pool = new HashMap<>();
        pool.put("initsize", "5");
        pool.put("maxsize", "20");
        ldapConfiguration.put("pool", pool);
        
        return ldapConfiguration;
    }
    
    
    private HashMap<String, Object> buildServerSnippet() {
        // server
        HashMap<String, Object> server = new HashMap<>();
        server.put("type", "simple");
        server.put("rootPath", "/api/");
        server.put("applicationContextPath", "/");
        //// connector
        HashMap<String, String> connector = new HashMap<>();
        connector.put("type", "http");
        connector.put("port", "8080");
        server.put("connector", connector);
        //// request log
        HashMap<String, Object> requestLog = new HashMap<>();
        requestLog.put("timeZone", "UTC");
        ArrayList<HashMap<String, String>> serverAppenders = new ArrayList<>();
        HashMap<String, String> typeLogServer = new HashMap<>();
        typeLogServer.put("type", "file");
        serverAppenders.add(typeLogServer);
        requestLog.put("appenders", serverAppenders);
        server.put("requestLog", requestLog);
        return server;
    }

    private HashMap<String, Object> buildLoggingSnippet() {
        // logging
        HashMap<String, Object> loggingValue = new HashMap<>();
        //appenders
        ArrayList<HashMap<String, String>> appenders = new ArrayList<>();
        HashMap<String, String> appenderParameters = new HashMap<>();
        appenderParameters.put("type", "console");
        appenders.add(appenderParameters);
        // loggers
        HashMap<String, String> loggers = new HashMap<>();
        loggers.put("com.vsct.dt", "INFO");

        // altogether
        loggingValue.put("loggers", loggers);
        loggingValue.put("appenders", appenders);
        loggingValue.put("level", "INFO");
        return loggingValue;
    }

    @Override
    public void run(Bootstrap bootstrap, Namespace namespace) throws Exception {
        String outputFile = namespace.getString("output-file");
        try (FileWriter outFile = new FileWriter(outputFile)) {

        outFile.write(generateConfiguration());
        outFile.flush();
        }
    }
}
