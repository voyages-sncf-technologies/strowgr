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

import com.github.tomakehurst.wiremock.WireMockServer;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.core.security.model.User;
import com.vsct.dt.strowgr.admin.gui.ConsulMockRule;
import com.vsct.dt.strowgr.admin.gui.StrowgrMain;
import com.vsct.dt.strowgr.admin.gui.configuration.StrowgrConfiguration;
import com.vsct.dt.strowgr.admin.gui.mapping.json.UpdatedEntryPointMappingJson;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Collections;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

public class EntryPointResourcesIT {

    private static final ConsulMockRule CONSUL_MOCK_RULE = new ConsulMockRule();

    private static final DropwizardAppRule<StrowgrConfiguration> ADMIN_RULE = new DropwizardAppRule<>(
            StrowgrMain.class, "src/main/resources/configuration.yaml"
    );

    @ClassRule
    public static RuleChain ruleChain = RuleChain.outerRule(CONSUL_MOCK_RULE).around(ADMIN_RULE);

    private static final WireMockServer CONSUL_MOCK = CONSUL_MOCK_RULE.getConsulMock();

    private static final WebTarget adminAppTarget = ClientBuilder.newClient()
            .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true)
            .property(ClientProperties.CONNECT_TIMEOUT, 1_000)
            .property(ClientProperties.READ_TIMEOUT, 5_000)
            .target("http://localhost:8080/");

    @Before
    public void setUp() throws Exception {

        CONSUL_MOCK_RULE.resetMocks();

        // session mocks for test/test entry point
        CONSUL_MOCK.stubFor(put(urlEqualTo("/v1/session/create"))
                .withRequestBody(equalTo("{\"Behavior\":\"release\",\"TTL\":\"10s\", \"Name\":\"test/test\", \"LockDelay\": \"0\" }"))
                .willReturn(aResponse().withBody("{\"ID\":\"session-id\"}")));
        CONSUL_MOCK.stubFor(put(urlEqualTo("/v1/session/destroy/session-id")).willReturn(aResponse()));
        CONSUL_MOCK.stubFor(put(urlEqualTo("/v1/kv/admin/test/test/lock?acquire=session-id")).willReturn(aResponse().withBody("true")));
        CONSUL_MOCK.stubFor(put(urlEqualTo("/v1/kv/admin/test/test/lock?release=session-id")).willReturn(aResponse()));
    }

    @Test
    public void swap_auto_reload_should_swap_key_in_consul() throws Exception {
        // given
        CONSUL_MOCK.stubFor(get(urlEqualTo("/v1/kv/admin/test/test/autoreload?raw")).willReturn(aResponse().withBody("false")));
        CONSUL_MOCK.stubFor(put(urlEqualTo("/v1/kv/admin/test/test/autoreload")).willReturn(aResponse()));

        // when
        Response response = adminAppTarget.path("/api/entrypoints/test/test/autoreload/swap")
                .request().method("PATCH");

        // then
        assertThat(response.getStatus()).isEqualTo(Status.PARTIAL_CONTENT.getStatusCode());
        CONSUL_MOCK.verify(putRequestedFor(urlEqualTo("/v1/kv/admin/test/test/autoreload"))
                .withRequestBody(equalTo("true")));
    }

    @Test
    public void add_entry_point_should_create_entry_point_in_consul() throws Exception {
        // given
        EntryPoint entryPoint = EntryPoint
                .onHaproxy("haproxy", 1)
                .withUser("someUser")
                .withVersion("someVersion")
                .definesFrontends(Collections.emptySet())
                .definesBackends(Collections.emptySet())
                .withGlobalContext(Collections.emptyMap())
                .build();

        CONSUL_MOCK.stubFor(get(urlEqualTo("/v1/kv/haproxy/haproxy/platform?raw")).willReturn(aResponse().withBody("test")));
        CONSUL_MOCK.stubFor(put(urlEqualTo("/v1/kv/admin/test/test/autoreload")).willReturn(aResponse()));
        CONSUL_MOCK.stubFor(get(urlEqualTo("/v1/kv/admin/test/test/current?raw")).willReturn(aResponse().withStatus(Status.NOT_FOUND.getStatusCode())));
        CONSUL_MOCK.stubFor(get(urlEqualTo("/v1/kv/admin/test/test/committing?raw")).willReturn(aResponse().withStatus(Status.NOT_FOUND.getStatusCode())));
        CONSUL_MOCK.stubFor(put(urlEqualTo("/v1/kv/admin/test/test/current")).willReturn(aResponse()));

        // when
        Response response = adminAppTarget.path("/api/entrypoints/test/test")
                .request().put(Entity.entity(entryPoint, MediaType.APPLICATION_JSON_TYPE));

        // then
        assertThat(response.getStatus()).isEqualTo(Status.CREATED.getStatusCode());
        CONSUL_MOCK.verify(putRequestedFor(urlEqualTo("/v1/kv/admin/test/test/autoreload"))
                .withRequestBody(equalTo("true")));
        CONSUL_MOCK.verify(putRequestedFor(urlEqualTo("/v1/kv/admin/test/test/current"))
                .withRequestBody(equalToJson("{\"haproxy\":\"haproxy\",\"hapUser\":\"someUser\",\"hapVersion\":\"someVersion\",\"bindingId\":1,\"frontends\":[],\"backends\":[],\"context\":{}}")));
    }

    @Test
    public void update_entry_point_should_update_entry_point_in_consul() throws Exception {
        // given
        UpdatedEntryPointMappingJson updatedEntryPointMappingJson = new UpdatedEntryPointMappingJson(User.UNTRACKED,
                "newUser", "newVersion", 0, Collections.emptyMap(),
                Collections.emptySet(), Collections.emptySet()
        );

        String actualVersion = "{\"haproxy\":\"horsprod\",\"hapUser\":\"someUser\",\"hapVersion\":\"someVersion\",\"bindingId\":0,\"frontends\":[],\"backends\":[],\"context\":{}}";
        CONSUL_MOCK.stubFor(get(urlEqualTo("/v1/kv/admin/test/test/pending?raw")).willReturn(aResponse().withBody(actualVersion)));
        CONSUL_MOCK.stubFor(get(urlEqualTo("/v1/kv/admin/test/test/committing?raw")).willReturn(aResponse().withBody(actualVersion)));
        CONSUL_MOCK.stubFor(put(urlEqualTo("/v1/kv/admin/test/test/pending")).willReturn(aResponse()));

        // when
        Response response = adminAppTarget.path("/api/entrypoints/test/test")
                .request() //.header(HttpHeaders.AUTHORIZATION, "bla")
                .method("PATCH", Entity.entity(updatedEntryPointMappingJson, MediaType.APPLICATION_JSON));

        // then
        assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
        CONSUL_MOCK.verify(putRequestedFor(urlEqualTo("/v1/kv/admin/test/test/pending"))
                .withRequestBody(equalToJson("{\"haproxy\":\"horsprod\",\"hapUser\":\"newUser\",\"hapVersion\":\"newVersion\",\"bindingId\":0,\"frontends\":[],\"backends\":[],\"context\":{}}")));
    }
}