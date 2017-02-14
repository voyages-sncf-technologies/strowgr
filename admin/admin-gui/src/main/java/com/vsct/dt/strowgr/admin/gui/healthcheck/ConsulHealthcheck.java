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
package com.vsct.dt.strowgr.admin.gui.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class ConsulHealthcheck extends HealthCheck {

    private final HttpGet httpGet;
    private final CloseableHttpClient httpClient;

    public ConsulHealthcheck(String host, int port) {
        httpClient = HttpClients.createDefault();
        httpGet = new HttpGet("http://" + host + ":" + port + "/");

    }

    @Override
    protected Result check() throws Exception {
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200)
                throw new IllegalStateException("http request on " + httpGet.getURI().toString() + " returns status " + response.getStatusLine());
            return Result.healthy();
        } catch (Exception e) {
            return Result.unhealthy("can't execute " + httpGet.getURI().toString());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }
}
