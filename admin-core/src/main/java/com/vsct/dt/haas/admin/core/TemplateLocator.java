package com.vsct.dt.haas.admin.core;

import com.vsct.dt.haas.admin.core.configuration.EntryPointConfiguration;

import java.io.Reader;

public interface TemplateLocator {
    Reader readTemplate(EntryPointConfiguration configuration);
}
