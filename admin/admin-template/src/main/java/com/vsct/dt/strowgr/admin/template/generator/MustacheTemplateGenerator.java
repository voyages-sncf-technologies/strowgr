package com.vsct.dt.strowgr.admin.template.generator;

import com.github.mustachejava.*;
import com.github.mustachejava.reflect.MissingWrapper;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.util.Wrapper;
import com.google.common.base.Preconditions;
import com.vsct.dt.strowgr.admin.core.TemplateGenerator;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.template.IncompleteConfigurationException;
import com.vsct.dt.strowgr.admin.template.locator.UriTemplateLocator;
import com.vsct.dt.strowgr.admin.template.template.DefaultTemplates;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MustacheTemplateGenerator implements TemplateGenerator {

    private final NoHTMLEscapingMustacheFactory mf = new NoHTMLEscapingMustacheFactory();

    @Override
    public String generate(String template, EntryPoint configuration, Map<String, Integer> portsMapping) throws IncompleteConfigurationException {
        Preconditions.checkNotNull(template, "template should not be null. Check uriTemplate %s is correct.", configuration.getContext().get(UriTemplateLocator.URI_FIELD));
        Writer writer = new StringWriter();

        RecordMissingEntriesObjectHandler objectHandler = new RecordMissingEntriesObjectHandler();
        mf.setObjectHandler(objectHandler);

        Mustache mustache = mf.compile(new StringReader(template), "no_cache");

        StrowgrMustacheScope scope = new StrowgrMustacheScope(configuration, portsMapping);

        mustache.execute(writer, scope);

        if (objectHandler.hasMissingEntries()) {
            throw new IncompleteConfigurationException(objectHandler.getMissingEntries());
        }

        return writer.toString();
    }

    @Override
    public String generateSyslogFragment(EntryPoint configuration, Map<String, Integer> portsMapping) {
        Writer writer = new StringWriter();
        RecordMissingEntriesObjectHandler objectHandler = new RecordMissingEntriesObjectHandler();
        mf.setObjectHandler(objectHandler);
        Mustache mustache = mf.compile(new StringReader(DefaultTemplates.SYSLOG_DEFAULT_TEMPLATE), "no_cache");
        mustache.execute(writer, new StrowgrMustacheScope(configuration, portsMapping));
        return writer.toString();
    }

    /**
     * Mustache ObjectHandler that records when an entry is missing
     * Implementation suppose a monothreaded template generation by mustache
     */
    private static class RecordMissingEntriesObjectHandler extends ReflectionObjectHandler {

        private Set<String> missingEntries = new HashSet<>();

        @Override
        public Wrapper find(final String name, List<Object> scopes) {
            Wrapper wrapper = super.find(name, scopes);
            if (wrapper instanceof MissingWrapper) {
                //return new RecordingMissingFieldWrapper(name, (MissingWrapper) wrapper);
                missingEntries.add(name);
            }
            return wrapper;
        }

        @Override
        public Writer falsey(Iteration iteration, Writer writer, Object object, List<Object> scopes) {
            if (iteration instanceof Code) {
                //There is a default behavior associated with the missing value, we should not raise an error
                missingEntries.remove(((Code) iteration).getName());
            }
            return super.falsey(iteration, writer, object, scopes);
        }

        public boolean hasMissingEntries() {
            return this.missingEntries.size() > 0;
        }

        public Set<String> getMissingEntries() {
            return missingEntries;
        }
    }

    private static class NoHTMLEscapingMustacheFactory extends DefaultMustacheFactory {

        @Override
        public void encode(String value, Writer writer) {
            try {
                writer.write(value);
            } catch (IOException e) {
                //Should never be here
                e.printStackTrace();
                throw new MustacheException("Impossible to execute encode method properly");
            }
        }
    }

}
