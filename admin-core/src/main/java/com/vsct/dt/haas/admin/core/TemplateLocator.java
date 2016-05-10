package com.vsct.dt.haas.admin.core;

import com.vsct.dt.haas.admin.core.configuration.EntryPoint;

import java.util.Optional;

public interface TemplateLocator {
    Optional<String> readTemplate(EntryPoint configuration);
}
