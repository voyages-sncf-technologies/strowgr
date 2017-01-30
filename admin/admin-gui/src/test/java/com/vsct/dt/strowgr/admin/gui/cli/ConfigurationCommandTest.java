package com.vsct.dt.strowgr.admin.gui.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigurationCommandTest {
    
    @Test
    public void should_generate_default_configuration_without_null() throws JsonProcessingException {
        // given
        ConfigurationCommand configurationCommand = new ConfigurationCommand();

        // test
        String configuration = configurationCommand.generateConfiguration();

        // check
        assertFalse(configuration.contains("null"));
    }
}