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
package com.vsct.dt.strowgr.admin.gui.resource.api;

import com.vsct.dt.strowgr.admin.core.TemplateGenerator;
import com.vsct.dt.strowgr.admin.template.locator.UriTemplateLocator;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class UriTemplateResourcesTest {

    static UriTemplateLocator   templateLocator      = mock(UriTemplateLocator.class);
    static TemplateGenerator    templateGenerator    = mock(TemplateGenerator.class);
    static UriTemplateResources uriTemplateResources = new UriTemplateResources(templateLocator, templateGenerator);

    @ClassRule
    public static ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(uriTemplateResources)
            .build();

    @Before
    public void setUp(){
    }

    @After
    public void tearDown(){
        reset(templateLocator, templateGenerator);
    }


    @Test
    public void should_get_template(){
        when(templateLocator.readTemplate("/some/uri")).thenReturn(Optional.of("I'm a template believe it or not"));

        String result = resources.client().target("/templates?uri=/some/uri").request().get(String.class);

        assertThat(result, is("I'm a template believe it or not"));
    }

    @Test
    public void get_template_should_return_404_if_not_found(){
        when(templateLocator.readTemplate("/some/uri")).thenReturn(Optional.empty());

        Response res = resources.client().target("/templates?uri=/some/uri").request().get();

        assertThat(res.getStatus(), is(404));
    }

    @Test
    public void should_get_frontends_and_backends_from_template(){
        Map<String, Set<String>> frontAndBackends = new HashMap<>();
        Set<String> backends = new HashSet<>();
        backends.add("backend");
        frontAndBackends.put("frontend", backends);

        when(templateLocator.readTemplate("/some/uri")).thenReturn(Optional.of("void"));
        when(templateGenerator.generateFrontAndBackends("void")).thenReturn(frontAndBackends);

        //jersey will prefer list impl instead of set, this is not a big deal since we just want to test size and presenc eof one element
        Map<String, List<String>> result = resources.client().target("/templates/frontbackends?uri=/some/uri").request().get(Map.class);

        assertThat(result.size(), is(1));
        assertThat(result.get("frontend").size(), is(1));
        assertThat(result.get("frontend").contains("backend"), is(true));
    }

    @Test
    public void get_frontends_and_backends_should_get_empty_front_and_backend_if_uri_is_not_found(){
        when(templateLocator.readTemplate("/some/uri")).thenReturn(Optional.empty());

        Response res = resources.client().target("/templates/frontbackends?uri=/some/uri").request().get();

        assertThat(res.getStatus(), is(404));
    }
}
