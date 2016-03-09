package com.vsct.dt.haas.admin.template;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import com.vsct.dt.haas.admin.core.configuration.EntryPointBackend;
import com.vsct.dt.haas.admin.core.configuration.EntryPointBackendServer;
import com.vsct.dt.haas.admin.core.configuration.EntryPointConfiguration;
import com.vsct.dt.haas.admin.core.configuration.EntryPointFrontend;
import com.vsct.dt.haas.admin.template.generator.MustacheTemplateGenerator;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class MustacheTemplateTest {

    MustacheTemplateGenerator templateGenerator = new MustacheTemplateGenerator();

    @Test
    public void should_valorise_template_with_standard_context() throws IOException {

        EntryPointFrontend frontend = new EntryPointFrontend("OCEREC1WS", Maps.newHashMap());

        EntryPointBackendServer server = new EntryPointBackendServer("instance_name", "server_name", "10.98.81.74", "9090", new HashMap<>(), new HashMap<>());
        EntryPointBackend backend = new EntryPointBackend("OCEREC1WS", Sets.newHashSet(server), Maps.newHashMap());
        Map<String, String> epContext = new HashMap<>();
        epContext.put("application", "OCE");
        epContext.put("platform", "REC1");
        EntryPointConfiguration configuration = new EntryPointConfiguration("default-name", "hapocer1", Sets.newHashSet(frontend), Sets.newHashSet(backend), epContext);

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("template.standard.context.mustache").getFile());
        FileReader reader = new FileReader(file);

        Map<String, Integer> portsMapping = new HashMap<>();
        portsMapping.put(configuration.syslogPortId(), 54250);
        portsMapping.put("OCEREC1WS", 50200);

        String result = templateGenerator.generate(CharStreams.toString(reader), configuration, portsMapping);

        File expectedF = new File(classLoader.getResource("template.standard.context.mustache.expected").getFile());
        reader = new FileReader(expectedF);

        String expected = CharStreams.toString(reader);
        reader.close();

        assertThat(result).isEqualTo(expected);
    }

    /* For the purpose of this test we chose to sort servers by id */
    @Test
    public void user_provided_context_for_servers_should_replace_server_provided_context() throws IOException {
        EntryPointFrontend frontend = new EntryPointFrontend("OCEREC1WS", Maps.newHashMap());

        Map<String, String> serverContext = new HashMap<>();serverContext.put("key1", "value1");serverContext.put("key2", "value2");
        Map<String, String> userContext1 = new HashMap<>();userContext1.put("key2", "user_value2");
        Map<String, String> userContext2 = new HashMap<>();userContext2.put("key1", "user_value1");

        EntryPointBackendServer server1 = new EntryPointBackendServer("instance_name_1", "server_name_1", "10.98.81.74", "9090", serverContext, userContext1);
        EntryPointBackendServer server2 = new EntryPointBackendServer("instance_name_2", "server_name_2", "10.98.81.75", "9090", serverContext, userContext2);
        EntryPointBackend backend = new EntryPointBackend("OCEREC1WS", Sets.newHashSet(server1, server2), Maps.newHashMap());
        Map<String, String> epContext = new HashMap<>();
        epContext.put("application", "OCE");
        epContext.put("platform", "REC1");
        EntryPointConfiguration configuration = new EntryPointConfiguration("default-name", "hapocer1", Sets.newHashSet(frontend), Sets.newHashSet(backend), epContext);

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("template.user.context.on.server.mustache").getFile());
        FileReader reader = new FileReader(file);

        Map<String, Integer> portsMapping = new HashMap<>();
        portsMapping.put(configuration.syslogPortId(), 54250);
        portsMapping.put("OCEREC1WS", 50200);

        String result = templateGenerator.generate(CharStreams.toString(reader), configuration, portsMapping);

        File expectedF = new File(classLoader.getResource("template.user.context.on.server.mustache.expected").getFile());
        reader = new FileReader(expectedF);

        String expected = CharStreams.toString(reader);
        reader.close();

        assertThat(result).isEqualTo(expected);
    }


}
