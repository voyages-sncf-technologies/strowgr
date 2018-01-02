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
package com.vsct.dt.strowgr.admin.nsq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQMessage;
import com.vsct.dt.strowgr.admin.core.event.in.CommitFailedEvent;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommitFailedTransformerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private final CommitFailedTransformer commitFailedTransformer = new CommitFailedTransformer(mapper);

    @Test
    public void should_transform_commit_failed_event_to_object() throws Exception {
        // given
        NSQMessage nsqMessage = mock(NSQMessage.class);
        when(nsqMessage.getMessage()).thenReturn(("" +
                "{" +
                "   'header':{" +
                "       'correlationId':'someCorrelationId'," +
                "      'application':'someApp'," +
                "       'platform':'somePlatform'" +
                "   }" +
                "}").replaceAll("'", "\"").getBytes());

        // when
        CommitFailedEvent result = commitFailedTransformer.apply(nsqMessage);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getCorrelationId()).isEqualTo("someCorrelationId");
        assertThat(result.getKey().getID()).isEqualTo("someApp/somePlatform");
    }
}