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
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class MustacheTemplateTest {

    MustacheTemplateGenerator templateGenerator = new MustacheTemplateGenerator();

    @Test
    public void testTemplate1() throws IOException {

        EntryPointFrontend frontend = new EntryPointFrontend("OCEREC1WS", "50200", Maps.newHashMap());

        EntryPointBackendServer server = new EntryPointBackendServer("instance_name", "server_name", "10.98.81.74", "9090");
        EntryPointBackend backend = new EntryPointBackend("OCEREC1WS", Sets.newHashSet(server), Maps.newHashMap());
        Map<String, String> epContext = new HashMap<>();
        epContext.put("application", "OCE");
        epContext.put("platform", "REC1");
        EntryPointConfiguration configuration = new EntryPointConfiguration("default-name", "hapocer1", "54250", Sets.newHashSet(frontend), Sets.newHashSet(backend), epContext);

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("template.without.context.mustache").getFile());
        FileReader reader = new FileReader(file);

        String result = templateGenerator.generate(reader, configuration);

        File expectedF = new File(classLoader.getResource("template.without.context.mustache.expected").getFile());
        reader = new FileReader(expectedF);

        String expected = CharStreams.toString(reader);
        reader.close();

        assertThat(result).isEqualTo(expected);
    }


}
