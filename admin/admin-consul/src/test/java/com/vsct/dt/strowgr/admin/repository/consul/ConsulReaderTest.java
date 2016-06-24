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

package com.vsct.dt.strowgr.admin.repository.consul;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicStatusLine;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConsulReaderTest {

    @Test
    public void should_return_value_when_status_in_range_200_299() throws ClientProtocolException {
        for (int status = 200; status < 300; status++) {
            // given
            checkValidStatus(status, false);
        }
    }

    @Test
    public void should_throw_exception_when_status_out_of_range_200_299() {
        for (int status = 100; status < 600; status++) {
            if (status >= 200 && status < 300) continue; // skip
            // given
            HttpResponse httpResponse = mock(HttpResponse.class);
            when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http1.1", 1, 1), status, ""));
            BasicHttpEntity givenHttpEntity = new BasicHttpEntity();
            givenHttpEntity.setContent(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
            when(httpResponse.getEntity()).thenReturn(givenHttpEntity);

            // test
            try {
                new ConsulReader(null).parseHttpResponse(httpResponse, this::getHttpEntity);
                // check
                fail("can't reach this point for status " + status);
            } catch (ClientProtocolException e) {
                // check
                assertThat(e.getMessage()).contains(String.valueOf(status));
            }
        }
    }

    @Test
    public void should_throw_exception_with_no_entity_when_status_out_of_range_200_299() {
        for (int status = 100; status < 600; status++) {
            if (status >= 200 && status < 300) continue; // skip
            // given
            HttpResponse httpResponse = mock(HttpResponse.class);
            when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http1.1", 1, 1), status, ""));

            // test
            try {
                new ConsulReader(null).parseHttpResponse(httpResponse, this::getHttpEntity);
                // check
                fail("can't reach this point for status " + status);
            } catch (ClientProtocolException e) {
                // check
                assertThat(e.getMessage()).contains(String.valueOf(status));
                assertThat(e.getMessage()).contains("no content");
            }
        }
    }


    @Test
    public void should_return_value_when_status_in_range_200_299_and_404() throws ClientProtocolException {
        for (int status = 200; status < 300; status++) {
            checkValidStatus(status, true);
        }
        checkValidStatus(404, true);
    }

    private void checkValidStatus(int status, boolean with404) throws ClientProtocolException {
        // given
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http1.1", 1, 1), status, ""));
        BasicHttpEntity givenHttpEntity = new BasicHttpEntity();
        givenHttpEntity.setContent(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
        when(httpResponse.getEntity()).thenReturn(givenHttpEntity);
        Optional<HttpEntity> httpEntity;

        // test
        if (with404) {
            httpEntity = new ConsulReader(null).parseHttpResponseAccepting404(httpResponse, this::getHttpEntity);
        } else {
            httpEntity = new ConsulReader(null).parseHttpResponse(httpResponse, this::getHttpEntity);
        }

        // check
        if (with404 && status == 404) {
            assertThat(httpEntity).isNotNull();
            assertThat(httpEntity.isPresent()).isFalse();
        } else {
            assertThat(httpEntity.isPresent()).as("for status " + status).isTrue();
            assertThat(httpEntity.orElseGet(() -> null)).as("for status " + status).isEqualTo(givenHttpEntity);
        }
    }

    private Optional<HttpEntity> getHttpEntity(HttpEntity httpEntity) {
        return Optional.of(httpEntity);
    }
}