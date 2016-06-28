/*
 *  Copyright (C) 2016 VSCT
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.vsct.dt.strowgr.admin.nsq.producer;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Http client for requesting an NSQ daemon.
 */
public class NSQHttpClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(NSQHttpClient.class);

    private final String uri;
    private final CloseableHttpClient httpClient;
    private final HttpGet httpPing;

    public NSQHttpClient(String uri, CloseableHttpClient httpClient) {
        this.uri = uri;
        this.httpClient = httpClient;
        httpPing = new HttpGet(uri + "/ping");
    }

    /**
     * Create a topic NSQ.
     *
     * @return {@code true} if topic creation succeed, {@code false} otherwise
     */
    public boolean createTopic(String topicName) {
        boolean topicCreated = false;
        LOGGER.debug("create topic {} on nsqd {}", uri, topicName);
        try (CloseableHttpResponse httpResponse = httpClient.execute(new HttpPost(uri + "/topic/create?topic=" + topicName))) {
            topicCreated = httpResponse.getStatusLine().getStatusCode() >= 200 && httpResponse.getStatusLine().getStatusCode() < 400;
            if (!topicCreated) {
                LOGGER.info("error {} when creating topic {}: {}", httpResponse.getStatusLine(), topicName, httpResponse);
            }
        } catch (IOException e) {
            LOGGER.error("can't create a topicName. Http request has failed", e);
        }
        return topicCreated;
    }

    /**
     * Ping NSQ daemon.
     *
     * @return {@code true} if ping succeed, {@code false} otherwise
     */
    public boolean ping() {
        boolean pinged = false;
        try (CloseableHttpResponse response = httpClient.execute(httpPing)) {
            pinged = response.getStatusLine().getStatusCode() == 200;
        } catch (Exception e) {
            LOGGER.error("can't ping NSQ in http", e);
        }
        return pinged;
    }
}
