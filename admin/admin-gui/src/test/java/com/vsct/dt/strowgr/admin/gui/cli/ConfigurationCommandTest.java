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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class ConfigurationCommandTest {
    
    @Test
    public void should_generate_default_configuration_without_null() throws JsonProcessingException {
        // given
        ConfigurationCommand configurationCommand = new ConfigurationCommand();

        // test
        String configuration = configurationCommand.generateConfiguration();
        
        System.out.println(configuration);

        // check
        assertFalse(configuration.contains("null"));
    }
}