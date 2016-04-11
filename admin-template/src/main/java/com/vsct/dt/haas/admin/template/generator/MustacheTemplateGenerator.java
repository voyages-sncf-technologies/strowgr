package com.vsct.dt.haas.admin.template.generator;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Preconditions;
import com.vsct.dt.haas.admin.core.TemplateGenerator;
import com.vsct.dt.haas.admin.core.configuration.EntryPoint;
import com.vsct.dt.haas.admin.template.locator.UriTemplateLocator;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import static com.vsct.dt.haas.admin.template.template.DefaultTemplates.SYSLOG_DEFAULT_TEMPLATE;

public class MustacheTemplateGenerator implements TemplateGenerator {

    private final MustacheFactory mf = new DefaultMustacheFactory();

    @Override
    public String generate(String template, EntryPoint configuration, Map<String, Integer> portsMapping) {
        Preconditions.checkNotNull(template, "template should not be null. Check uriTemplate %s is correct.", configuration.getContext().get(UriTemplateLocator.URI_FIELD));
        Writer writer = new StringWriter();
        Mustache mustache = mf.compile(new StringReader(template), "no_cache");
        mustache.execute(writer, new HaasMustacheScope(configuration, portsMapping));
        return writer.toString();
    }

    @Override
    public String generateSyslogFragment(EntryPoint configuration, Map<String, Integer> portsMapping) {
        Writer writer = new StringWriter();
        Mustache mustache = mf.compile(new StringReader(SYSLOG_DEFAULT_TEMPLATE), "no_cache");
        mustache.execute(writer, new HaasMustacheScope(configuration, portsMapping));
        return writer.toString();
    }
}
