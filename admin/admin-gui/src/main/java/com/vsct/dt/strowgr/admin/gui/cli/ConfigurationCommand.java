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

package com.vsct.dt.strowgr.admin.gui.cli;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationCommand extends Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationCommand.class);

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

    public Map<String, String> defaultValues() {
        HashMap<String, String> defaultValues = new HashMap<>();
        defaultValues.put("server.connector.port", "50090");
        defaultValues.put("repository.host", "localhost");
        defaultValues.put("repository.port", "8500");
        defaultValues.put("nsqLookup.host", "localhost");
        defaultValues.put("nsqLookup.port", "4161");
        defaultValues.put("nsqProducer.host", "localhost");
        defaultValues.put("nsqProducer.tcpPort", "4150");
        defaultValues.put("nsqProducer.httpPort", "4151");

        return defaultValues;
    }

    @Override
    public void run(Bootstrap bootstrap, Namespace namespace) throws Exception {
        String outputFile = namespace.getString("output-file");
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile("admin.yaml.mustach");
        Map<String, String> properties = defaultValues();
        for (Map.Entry<Object, Object> value : System.getProperties().entrySet()) {
            properties.put((String) value.getKey(), (String) value.getValue());
            LOGGER.debug("add property {} with value {}", value.getKey(), value.getValue());
        }
        mustache.execute(new FileWriter(outputFile), properties).flush();
    }
}
