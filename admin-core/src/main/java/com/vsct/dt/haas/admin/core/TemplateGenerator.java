package com.vsct.dt.haas.admin.core;

import com.vsct.dt.haas.admin.core.configuration.EntryPoint;

import java.util.Map;

public interface TemplateGenerator {
    String generate(String template, EntryPoint configuration, Map<String, Integer> portsMapping);

    String generateSyslogFragment(EntryPoint configuration, Map<String, Integer> portsMapping);

}