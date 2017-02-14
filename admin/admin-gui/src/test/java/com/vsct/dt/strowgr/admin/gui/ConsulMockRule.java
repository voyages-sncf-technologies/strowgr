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
package com.vsct.dt.strowgr.admin.gui;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Encoding;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class ConsulMockRule implements TestRule {

    private final WireMockServer consulMock = new WireMockServer(options().port(8500));

    private String consulKey(String key, String value) {
        return "[{\"LockIndex\":0,\"Key\":\"" + key + "\",\"Flags\":0,\"Value\":\"" + Encoding.encodeBase64(value.getBytes()) + "\",\"CreateIndex\":0,\"ModifyIndex\":0}]";
    }

    public void resetMocks() {

        // reset all mocks
        consulMock.resetAll();

        // init and scheduled resources
        consulMock.stubFor(get(urlEqualTo("/v1/kv/ports"))
                .willReturn(aResponse().withBody(consulKey("ports", "{}"))));
        consulMock.stubFor(get(urlEqualTo("/v1/kv/admin?keys"))
                .willReturn(aResponse().withBody("[]")));
        consulMock.stubFor(get(urlEqualTo("/v1/kv/haproxy/?raw&recurse=true"))
                .willReturn(aResponse().withBody("[]")));
        consulMock.stubFor(get(urlEqualTo("/v1/kv/haproxy/"))
                .willReturn(aResponse().withBody(consulKey("haproxy", ""))));
        consulMock.stubFor(get(urlEqualTo("/v1/kv/admin/"))
                .willReturn(aResponse().withBody(consulKey("admin", ""))));
        consulMock.stubFor(get(urlEqualTo("/v1/kv/haproxyversions"))
                .willReturn(aResponse().withBody(consulKey("haproxyversions", "[]"))));

    }

    @Override
    public Statement apply(Statement statement, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {

                    consulMock.start();
                    resetMocks();

                    statement.evaluate();

                } finally {
                    consulMock.stop();
                }
            }
        };
    }

    public WireMockServer getConsulMock() {
        return consulMock;
    }

}
