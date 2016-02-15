package com.vsct.dt.haas.admin.core;

import com.vsct.dt.haas.admin.core.configuration.EntryPointConfiguration;

import java.io.Reader;

public interface TemplateGenerator {
    String generate(Reader template, EntryPointConfiguration configuration);
}
