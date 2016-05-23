package com.vsct.dt.strowgr.admin.core;

import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;

import java.util.Optional;

public interface TemplateLocator {
    Optional<String> readTemplate(EntryPoint configuration);
}
