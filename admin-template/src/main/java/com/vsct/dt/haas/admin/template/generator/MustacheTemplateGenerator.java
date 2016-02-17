package com.vsct.dt.haas.admin.template.generator;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.vsct.dt.haas.admin.core.TemplateGenerator;
import com.vsct.dt.haas.admin.core.configuration.EntryPointConfiguration;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

public class MustacheTemplateGenerator implements TemplateGenerator {

    private final MustacheFactory mf = new DefaultMustacheFactory();

    @Override
    public String generate(String template, EntryPointConfiguration configuration) {
        Writer writer = new StringWriter();
        Mustache mustache = mf.compile(new StringReader(template), "no_cache");
        mustache.execute(writer, new HaasMustacheScope(configuration));
        return writer.toString();
    }
}
