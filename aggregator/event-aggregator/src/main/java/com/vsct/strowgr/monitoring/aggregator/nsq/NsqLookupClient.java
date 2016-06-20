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

package com.vsct.strowgr.monitoring.aggregator.nsq;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

public class NsqLookupClient {

    private static final Logger LOGGER          = LoggerFactory.getLogger(NsqLookupClient.class);
    private static final String TOPICS_ENDPOINT = "/topics";

    private final CloseableHttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String host;
    private final int    port;

    public NsqLookupClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.client = HttpClients.createDefault();
    }

    public Set<String> getTopics() throws UnavailableNsqException {
        try {
            LOGGER.info("Listing all nsq topics");
            HttpGet uri = new HttpGet("http://" + host + ":" + port + TOPICS_ENDPOINT);
            return client.execute(uri, new ResponseHandler<Set<String>>() {
                @Override
                public Set<String> handleResponse(HttpResponse httpResponse) throws ClientProtocolException, IOException {
                    int status = httpResponse.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = httpResponse.getEntity();
                        NsqLookupResponse<NsqLookupTopics> nsqResponse = mapper.readValue(entity.getContent(), new TypeReference<NsqLookupResponse<NsqLookupTopics>>() {
                        });
                        return nsqResponse.data.topics;
                    }
                    else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }
            });
        } catch (IOException e) {
            LOGGER.error("error while querying nsqlookup for list of topics");
            throw new UnavailableNsqException(e);
        }
    }

    private static class NsqLookupResponse<T> {

        @JsonProperty("status_code")
        String statusCode;

        @JsonProperty("status_txt")
        String statusTxt;

        @JsonProperty("data")
        T data;

    }

    private static class NsqLookupTopics {
        @JsonProperty("topics")
        Set<String> topics;
    }


}
