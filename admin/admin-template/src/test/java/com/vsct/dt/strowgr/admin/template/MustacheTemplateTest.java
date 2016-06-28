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

package com.vsct.dt.strowgr.admin.template;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPointBackend;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPointBackendServer;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPointFrontend;
import com.vsct.dt.strowgr.admin.template.generator.MustacheTemplateGenerator;
import org.fest.assertions.Assertions;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class MustacheTemplateTest {

    MustacheTemplateGenerator templateGenerator = new MustacheTemplateGenerator();

    @Test
    public void should_valorise_template_with_standard_context() throws IOException, IncompleteConfigurationException {

        EntryPointFrontend frontend = new EntryPointFrontend("OCEREC1WS", Maps.newHashMap());

        EntryPointBackendServer server = new EntryPointBackendServer("instance_name", "10.98.81.74", "9090", new HashMap<>(), new HashMap<>());
        EntryPointBackend backend = new EntryPointBackend("OCEREC1WS", Sets.newHashSet(server), Maps.newHashMap());
        Map<String, String> epContext = new HashMap<>();
        epContext.put("application", "OCE");
        epContext.put("platform", "REC1");
        EntryPoint configuration = new EntryPoint("default-name", "hapocer1", Sets.newHashSet(frontend), Sets.newHashSet(backend), epContext);

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

    @Test
    public void should_valorise_template_with_iteration_over_all_frontends_and_all_backends() throws IOException, IncompleteConfigurationException {

        EntryPointFrontend frontend1 = new EntryPointFrontend("OCEREC1WS1", Maps.newHashMap());
        EntryPointFrontend frontend2 = new EntryPointFrontend("OCEREC1WS2", Maps.newHashMap());

        EntryPointBackendServer server11 = new EntryPointBackendServer("instance_name1_ws1", "10.98.81.74", "9090", new HashMap<>(), new HashMap<>());
        EntryPointBackendServer server12 = new EntryPointBackendServer("instance_name2_ws1",  "10.98.81.74", "9091", new HashMap<>(), new HashMap<>());

        EntryPointBackend backend1 = new EntryPointBackend("OCEREC1WS1", Sets.newHashSet(server11, server12), Maps.newHashMap());

        EntryPointBackendServer server21 = new EntryPointBackendServer("instance_name1_ws2",  "10.98.81.75", "9090", new HashMap<>(), new HashMap<>());
        EntryPointBackendServer server22 = new EntryPointBackendServer("instance_name2_ws2",  "10.98.81.75", "9091", new HashMap<>(), new HashMap<>());

        EntryPointBackend backend2 = new EntryPointBackend("OCEREC1WS2", Sets.newHashSet(server21, server22), Maps.newHashMap());
        Map<String, String> epContext = new HashMap<>();
        epContext.put("application", "OCE");
        epContext.put("platform", "REC1");
        EntryPoint configuration = new EntryPoint("default-name", "hapocer1", Sets.newHashSet(frontend1, frontend2), Sets.newHashSet(backend1, backend2), epContext);

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("template.iterate.servers.context.mustache").getFile());
        FileReader reader = new FileReader(file);

        Map<String, Integer> portsMapping = new HashMap<>();
        portsMapping.put(configuration.syslogPortId(), 54250);
        portsMapping.put("OCEREC1WS1", 50200);
        portsMapping.put("OCEREC1WS2", 50201);

        String result = templateGenerator.generate(CharStreams.toString(reader), configuration, portsMapping);

        File expectedF = new File(classLoader.getResource("template.iterate.servers.context.mustache.expected").getFile());
        reader = new FileReader(expectedF);

        String expected = CharStreams.toString(reader);
        reader.close();

        assertThat(result).isEqualTo(expected);
    }

    /* For the purpose of this test we chose to sort servers by id */
    @Test
    public void user_provided_context_for_servers_should_replace_server_provided_context() throws IOException, IncompleteConfigurationException {
        EntryPointFrontend frontend = new EntryPointFrontend("OCEREC1WS", Maps.newHashMap());

        Map<String, String> serverContext = new HashMap<>();
        serverContext.put("key1", "value1");
        serverContext.put("key2", "value2");
        Map<String, String> userContext1 = new HashMap<>();
        userContext1.put("key2", "user_value2");
        Map<String, String> userContext2 = new HashMap<>();
        userContext2.put("key1", "user_value1");

        EntryPointBackendServer server1 = new EntryPointBackendServer("instance_name_1", "10.98.81.74", "9090", serverContext, userContext1);
        EntryPointBackendServer server2 = new EntryPointBackendServer("instance_name_2", "10.98.81.75", "9090", serverContext, userContext2);
        EntryPointBackend backend = new EntryPointBackend("OCEREC1WS", Sets.newHashSet(server1, server2), Maps.newHashMap());
        Map<String, String> epContext = new HashMap<>();
        epContext.put("application", "OCE");
        epContext.put("platform", "REC1");
        EntryPoint configuration = new EntryPoint("default-name", "hapocer1", Sets.newHashSet(frontend), Sets.newHashSet(backend), epContext);

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

    @Test(expected = IncompleteConfigurationException.class)
    public void should_throw_excpetion_when_port_is_missing_or_null() throws IOException, IncompleteConfigurationException {

        EntryPointFrontend frontend1 = new EntryPointFrontend("OCEREC1WS1", Maps.newHashMap());
        EntryPointFrontend frontend2 = new EntryPointFrontend("OCEREC1WS2", Maps.newHashMap());

        EntryPointBackendServer server11 = new EntryPointBackendServer("instance_name1_ws1",  "10.98.81.74", "9090", new HashMap<>(), new HashMap<>());
        EntryPointBackendServer server12 = new EntryPointBackendServer("instance_name2_ws1",  "10.98.81.74", "9091", new HashMap<>(), new HashMap<>());

        EntryPointBackend backend1 = new EntryPointBackend("OCEREC1WS1", Sets.newHashSet(server11, server12), Maps.newHashMap());

        EntryPointBackendServer server21 = new EntryPointBackendServer("instance_name1_ws2", "10.98.81.75", "9090", new HashMap<>(), new HashMap<>());
        EntryPointBackendServer server22 = new EntryPointBackendServer("instance_name2_ws2", "10.98.81.75", "9091", new HashMap<>(), new HashMap<>());

        EntryPointBackend backend2 = new EntryPointBackend("OCEREC1WS2", Sets.newHashSet(server21, server22), Maps.newHashMap());
        Map<String, String> epContext = new HashMap<>();
        epContext.put("application", "OCE");
        epContext.put("platform", "REC1");
        EntryPoint configuration = new EntryPoint("default-name", "hapocer1", Sets.newHashSet(frontend1, frontend2), Sets.newHashSet(backend1, backend2), epContext);

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("template.iterate.servers.context.mustache").getFile());
        FileReader reader = new FileReader(file);

        Map<String, Integer> portsMapping = new HashMap<>();
        portsMapping.put(configuration.syslogPortId(), 54250);
        portsMapping.put("OCEREC1WS2", null);

        templateGenerator.generate(CharStreams.toString(reader), configuration, portsMapping);
    }

    @Test(expected = IncompleteConfigurationException.class)
    public void should_throw_exception_when_variable_is_missing_to_valorize_template_attempt1_missing_global_entry() throws IOException, IncompleteConfigurationException {
        EntryPointFrontend frontend = new EntryPointFrontend("OCEREC1WS", Maps.newHashMap());

        Map<String, String> serverContext = new HashMap<>();
        serverContext.put("maxconn", "50");
        EntryPointBackendServer server = new EntryPointBackendServer("instance_name", "10.98.81.74", "9090", new HashMap<>(), serverContext);
        EntryPointBackend backend = new EntryPointBackend("OCEREC1WS", Sets.newHashSet(server), Maps.newHashMap());
        Map<String, String> epContext = new HashMap<>();
        epContext.put("application", "OCE");
        epContext.put("platform", "REC1");
        EntryPoint configuration = new EntryPoint("default-name", "hapocer1", Sets.newHashSet(frontend), Sets.newHashSet(backend), epContext);

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("template.missing.property.1.context.mustache").getFile());
        FileReader reader = new FileReader(file);

        Map<String, Integer> portsMapping = new HashMap<>();
        portsMapping.put(configuration.syslogPortId(), 54250);
        portsMapping.put("OCEREC1WS", 50200);

        try {
            templateGenerator.generate(CharStreams.toString(reader), configuration, portsMapping);
        } catch (IncompleteConfigurationException e) {
            System.out.println(e.getMessage());
            Assertions.assertThat(e.getMissingEntries().size()).isEqualTo(2);
            Assertions.assertThat(e.getMissingEntries().contains("missing_entry.value")).isTrue();
            Assertions.assertThat(e.getMissingEntries().contains("missing_log")).isTrue();
            throw e;
        }
    }

    @Test(expected = IncompleteConfigurationException.class)
    public void should_throw_exception_when_variable_is_missing_to_valorize_template_attempt2_missing_front_entry() throws IOException, IncompleteConfigurationException {
        EntryPointFrontend frontend = new EntryPointFrontend("OCEREC1WS", Maps.newHashMap());

        Map<String, String> serverContext = new HashMap<>();
        serverContext.put("maxconn", "50");
        EntryPointBackendServer server = new EntryPointBackendServer("instance_name","10.98.81.74", "9090", new HashMap<>(), serverContext);
        EntryPointBackend backend = new EntryPointBackend("OCEREC1WS", Sets.newHashSet(server), Maps.newHashMap());
        Map<String, String> epContext = new HashMap<>();
        epContext.put("application", "OCE");
        epContext.put("platform", "REC1");
        EntryPoint configuration = new EntryPoint("default-name", "hapocer1", Sets.newHashSet(frontend), Sets.newHashSet(backend), epContext);

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("template.missing.property.2.context.mustache").getFile());
        FileReader reader = new FileReader(file);

        Map<String, Integer> portsMapping = new HashMap<>();
        portsMapping.put(configuration.syslogPortId(), 54250);
        portsMapping.put("OCEREC1WS", 50200);

        try {
            templateGenerator.generate(CharStreams.toString(reader), configuration, portsMapping);
        } catch (IncompleteConfigurationException e) {
            System.out.println(e.getMessage());
            Assertions.assertThat(e.getMissingEntries().size()).isEqualTo(1);
            Assertions.assertThat(e.getMissingEntries().contains("frontend.MISSING_FRONT.port")).isTrue();
            throw e;
        }
    }

    @Test(expected = IncompleteConfigurationException.class)
    public void should_throw_exception_when_variable_is_missing_to_valorize_template_attempt31_missing_server_entry() throws IOException, IncompleteConfigurationException {
        EntryPointFrontend frontend = new EntryPointFrontend("OCEREC1WS", Maps.newHashMap());

        Map<String, String> serverContext = new HashMap<>();
        serverContext.put("maxconn", "50");
        EntryPointBackendServer server = new EntryPointBackendServer("instance_name", "10.98.81.74", "9090", new HashMap<>(), serverContext);
        EntryPointBackend backend = new EntryPointBackend("OCEREC1WS", Sets.newHashSet(server), Maps.newHashMap());
        Map<String, String> epContext = new HashMap<>();
        epContext.put("application", "OCE");
        epContext.put("platform", "REC1");
        EntryPoint configuration = new EntryPoint("default-name", "hapocer1", Sets.newHashSet(frontend), Sets.newHashSet(backend), epContext);

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("template.missing.property.3.context.mustache").getFile());
        FileReader reader = new FileReader(file);

        Map<String, Integer> portsMapping = new HashMap<>();
        portsMapping.put(configuration.syslogPortId(), 54250);
        portsMapping.put("OCEREC1WS", 50200);

        try {
            templateGenerator.generate(CharStreams.toString(reader), configuration, portsMapping);
        } catch (IncompleteConfigurationException e) {
            System.out.println(e.getMessage());
            Assertions.assertThat(e.getMissingEntries().size()).isEqualTo(1);
            Assertions.assertThat(e.getMissingEntries().contains("missing_server_prop")).isTrue();
            throw e;
        }
    }

    @Test
    public void should_not_throw_exception_when_variable_is_missing_but_default_behavior_exists() throws IOException {
        EntryPointFrontend frontend = new EntryPointFrontend("OCEREC1WS", Maps.newHashMap());

        EntryPointBackendServer server = new EntryPointBackendServer("instance_name", "10.98.81.74", "9090", new HashMap<>(), new HashMap<>());
        EntryPointBackend backend = new EntryPointBackend("OCEREC1WS", Sets.newHashSet(server), Maps.newHashMap());
        Map<String, String> epContext = new HashMap<>();
        epContext.put("application", "OCE");
        epContext.put("platform", "REC1");
        EntryPoint configuration = new EntryPoint("default-name", "hapocer1", Sets.newHashSet(frontend), Sets.newHashSet(backend), epContext);

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("template.missing.property.with.default.mustache").getFile());
        FileReader reader = new FileReader(file);

        Map<String, Integer> portsMapping = new HashMap<>();
        portsMapping.put(configuration.syslogPortId(), 54250);
        portsMapping.put("OCEREC1WS", 50200);

        templateGenerator.generate(CharStreams.toString(reader), configuration, portsMapping);
    }
}
