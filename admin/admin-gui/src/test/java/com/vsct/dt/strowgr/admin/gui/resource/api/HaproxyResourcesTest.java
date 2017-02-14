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

import com.vsct.dt.strowgr.admin.core.IncompleteConfigurationException;
import com.vsct.dt.strowgr.admin.core.TemplateGenerator;
import com.vsct.dt.strowgr.admin.core.repository.HaproxyRepository;
import com.vsct.dt.strowgr.admin.gui.mapping.json.EntryPointWithPortsMappingJson;
import com.vsct.dt.strowgr.admin.gui.mapping.json.HaproxyMappingJson;
import com.vsct.dt.strowgr.admin.template.locator.UriTemplateLocator;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class HaproxyResourcesTest {

    static HaproxyRepository haproxyRepository = mock(HaproxyRepository.class);
    static UriTemplateLocator templateLocator = mock(UriTemplateLocator.class);
    static TemplateGenerator templateGenerator = mock(TemplateGenerator.class);
    static HaproxyResources haproxyResources = new HaproxyResources(haproxyRepository, templateLocator, templateGenerator);

    @ClassRule
    public static ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(haproxyResources)
            .build();

    @Before
    public void setUp(){
    }

    @After
    public void tearDown(){
        reset(haproxyRepository, templateLocator, templateGenerator);
    }

    @Test
    public void should_create_haproxy_with_all_properties(){
        Map<Integer, String> bindings = new HashMap<>();
        bindings.put(0, "vip0");
        bindings.put(1, "vip1");
        HaproxyMappingJson haproxyJson = new HaproxyMappingJson("name", bindings, "platform", true);

        Response res = resources.client().target("/haproxy/id").request().put(Entity.json(haproxyJson));

        assertThat(res.getStatus(), is(201));
        assertThat(res.getHeaderString("location"), is(resources.getJerseyTest().target("/haproxy/id").getUri().toString()));

        verify(haproxyRepository).setHaproxyProperty("id", "name", "name");
        verify(haproxyRepository).setHaproxyProperty("id", "platform", "platform");
        verify(haproxyRepository).setHaproxyProperty("id", "binding/0", "vip0");
        verify(haproxyRepository).setHaproxyProperty("id", "binding/1", "vip1");
        verify(haproxyRepository).setHaproxyProperty("id", "autoreload", "true");
    }

    @Test
    public void should_set_haproxy_ip_binding(){
        Response res = resources.client().target("/haproxy/id/binding/0").request().put(Entity.text("192.168.0.1"));

        assertThat(res.getStatus(), is(201));
        assertThat(res.getHeaderString("location"), is(resources.getJerseyTest().target("/haproxy/id/binding/0").getUri().toString()));

        verify(haproxyRepository).setHaproxyProperty("id", "binding/0", "192.168.0.1");
    }

    @Test
    public void should_get_ip_bindings(){
        when(haproxyRepository.getHaproxyProperty("id", "binding/0")).thenReturn(Optional.of("vip1"));
        String value = resources.client().target("/haproxy/id/binding/0").request().get(String.class);

        assertThat(value, is("vip1"));
    }

    @Test
    public void get_ip_bindings_with_invalid_id_should_return_400(){
        when(haproxyRepository.getHaproxyProperty("id", "binding/0")).thenReturn(Optional.empty());

        Response res = resources.client().target("/haproxy/id/binding/0").request().get();
        assertThat(res.getStatus(), is(404));
    }

    @Test
    public void should_get_haproxy(){
        Map<String, String> props = new HashMap<>();
        props.put("prop1", "value1");
        props.put("prop2", "value2");

        when(haproxyRepository.getHaproxyProperties("id")).thenReturn(Optional.of(props));

        Map<String, String> result = resources.client().target("/haproxy/id").request().get(Map.class);

        assertThat(result, is(props));
    }

    @Test
    public void get_haproxy_with_unknown_id_should_return_404(){
        when(haproxyRepository.getHaproxyProperties("id")).thenReturn(Optional.empty());
        Response res = resources.client().target("/haproxy/id").request().get();
        assertThat(res.getStatus(), is(404));
    }

    @Test
    public void should_get_all_haproxies(){
        Map<String, String> pa = new HashMap<>();
        pa.put("prop1", "value1");
        pa.put("prop2", "value2");

        Map<String, String> pb = new HashMap<>();
        pb.put("prop1", "value1");
        pb.put("prop2", "value2");

        List<Map<String, String>> allProps = new ArrayList<>();
        allProps.add(pa);
        allProps.add(pb);

        when(haproxyRepository.getHaproxyProperties()).thenReturn(allProps);

        List<Map<String, String>> result = resources.client().target("/haproxy").request().get(List.class);

        assertThat(result, is(allProps));
    }

    @Test
    public void should_get_all_ids(){

        Set<String> ids = new HashSet<>();
        ids.add("id1");
        ids.add("id2");
        when(haproxyRepository.getHaproxyIds()).thenReturn(ids);

        Set<String> result = resources.client().target("/haproxy/ids").request().get(Set.class);

        assertThat(result, is(ids));
    }

    @Test
    public void should_get_haproxy_configuration_from_entrypoint_configuration() throws IncompleteConfigurationException {
        //for unknown reason, this jersey testing has a problem when you set a number for the property syslogPort.
        //this may be due to class hierarchy with json mappings
        //For this reasons we pass null value.
        EntryPointWithPortsMappingJson ep = new EntryPointWithPortsMappingJson("haproxy", "user", "haproxyVersion", 0, null, new HashSet<>(), new HashSet<>(), new HashMap<>());
        when(templateLocator.readTemplate(ep)).thenReturn(Optional.of("A template"));
        when(templateGenerator.generate("A template", ep, ep.generatePortMapping())).thenReturn("A valorized template");

        String result = resources.client().target("/haproxy/template/valorise").request().post(Entity.json(ep), String.class);

        assertThat(result, is("A valorized template"));
    }

    @Test
    public void get_haproxy_configuration_should_return_404_if_template_not_found(){
        EntryPointWithPortsMappingJson ep = new EntryPointWithPortsMappingJson("haproxy", "user", "haproxyVersion", 0, null, new HashSet<>(), new HashSet<>(), new HashMap<>());
        when(templateLocator.readTemplate(ep)).thenReturn(Optional.empty());

        Response res = resources.client().target("/haproxy/template/valorise").request().post(Entity.json(ep));

        assertThat(res.getStatus(), is(404));
    }

    @Test
    public void get_haproxy_configuration_should_return_400_if_template_cannot_be_properly_valorised() throws IncompleteConfigurationException {
        EntryPointWithPortsMappingJson ep = new EntryPointWithPortsMappingJson("haproxy", "user", "haproxyVersion", 0, null, new HashSet<>(), new HashSet<>(), new HashMap<>());
        when(templateLocator.readTemplate(ep)).thenReturn(Optional.of("A template"));
        IncompleteConfigurationException ex = new IncompleteConfigurationException(new HashSet<String>());
        when(templateGenerator.generate("A template", ep, ep.generatePortMapping())).thenThrow(ex);

        Response res = resources.client().target("/haproxy/template/valorise").request().post(Entity.json(ep));

        assertThat(res.getStatus(), is(400));
    }

}
