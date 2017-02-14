/*
 * Copyright (C) 2016 VSCT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vsct.dt.strowgr.admin.template.generator;

import com.github.mustachejava.*;
import com.github.mustachejava.reflect.MissingWrapper;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.util.Wrapper;
import com.google.common.base.Preconditions;
import com.vsct.dt.strowgr.admin.core.IncompleteConfigurationException;
import com.vsct.dt.strowgr.admin.core.TemplateGenerator;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.template.locator.UriTemplateLocator;
import com.vsct.dt.strowgr.admin.template.template.DefaultTemplates;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

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

    @Override
    public Map<String, Set<String>> generateFrontAndBackends(String template) {
        HashMap<String, Set<String>> result = new HashMap<>();
        result.put("frontends", new HashSet<>());
        result.put("backends", new HashSet<>());
        Mustache mustache = mf.compile(new StringReader(template), "no_cache");
        for (Code code : mustache.getCodes()) {
            if (code != null && code.getName() != null) {
                if (code.getName().startsWith("backend") && code.getName().split("\\.").length > 1) {
                    result.get("backends").add(code.getName().split("\\.")[1]);
                } else if (code.getName().startsWith("frontend") && code.getName().split("\\.").length > 1) {
                    result.get("frontends").add(code.getName().split("\\.")[1]);
                }
            }
        }
        return result;
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
