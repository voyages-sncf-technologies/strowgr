package com.vsct.dt.haas.admin.core;

import com.vsct.dt.haas.admin.core.configuration.EntryPoint;

public interface TemplateLocator {
    String readTemplate(EntryPoint configuration);
}
