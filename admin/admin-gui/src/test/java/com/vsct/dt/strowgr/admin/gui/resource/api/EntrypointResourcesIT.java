package com.vsct.dt.strowgr.admin.gui.resource.api;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.vsct.dt.strowgr.admin.gui.ConsulMockRule;
import com.vsct.dt.strowgr.admin.gui.StrowgrMain;
import com.vsct.dt.strowgr.admin.gui.configuration.StrowgrConfiguration;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class EntrypointResourcesIT {

    private static final ConsulMockRule CONSUL_MOCK_RULE = new ConsulMockRule();

    private static final DropwizardAppRule<StrowgrConfiguration> ADMIN_RULE = new DropwizardAppRule<>(
            StrowgrMain.class, "src/main/resources/configuration.yaml"
    );

    @ClassRule
    public static RuleChain ruleChain = RuleChain.outerRule(CONSUL_MOCK_RULE).around(ADMIN_RULE);

    private final WebTarget adminAppTarget = ClientBuilder.newClient()
            .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true)
            .property(ClientProperties.CONNECT_TIMEOUT, 1_000)
            .property(ClientProperties.READ_TIMEOUT, 5_000)
            .target("http://localhost:8080/");

    @Test
    public void swap_auto_reload_should_swap_key_in_consul() throws Exception {
        // given
        WireMockServer consulMock = CONSUL_MOCK_RULE.getConsulMock();

        consulMock.stubFor(put(urlEqualTo("/v1/session/create"))
                .withRequestBody(equalTo("{\"Behavior\":\"release\",\"TTL\":\"10s\", \"Name\":\"test/test\", \"LockDelay\": \"0\" }"))
                .willReturn(aResponse().withBody("{\"ID\":\"session-id\"}")));

        consulMock.stubFor(put(urlEqualTo("/v1/session/destroy/session-id")).willReturn(aResponse()));

        consulMock.stubFor(put(urlEqualTo("/v1/kv/admin/test/test/lock?acquire=session-id")).willReturn(aResponse().withBody("true")));
        consulMock.stubFor(put(urlEqualTo("/v1/kv/admin/test/test/lock?release=session-id")).willReturn(aResponse()));
        consulMock.stubFor(get(urlEqualTo("/v1/kv/admin/test/test/autoreload?raw")).willReturn(aResponse().withBody("false")));
        consulMock.stubFor(put(urlEqualTo("/v1/kv/admin/test/test/autoreload")).willReturn(aResponse()));

        // when
        adminAppTarget.path("/api/entrypoints/test/test/autoreload/swap").request().method("PATCH");

        // then
        consulMock.verify(putRequestedFor(urlEqualTo("/v1/kv/admin/test/test/autoreload")).withRequestBody(equalTo("true")));
    }
}