package com.vsct.dt.haas.admin.core;

import com.vsct.dt.haas.admin.core.configuration.EntryPointConfiguration;

import java.io.Reader;
import java.util.Map;

public interface TemplateGenerator {
    String generate(String template, EntryPointConfiguration configuration, Map<String, Integer> portsMapping);

    String generateSyslogFragment(EntryPointConfiguration configuration, Map<String, Integer> portsMapping);

}