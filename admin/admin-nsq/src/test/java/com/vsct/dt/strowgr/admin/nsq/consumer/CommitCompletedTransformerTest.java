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
import com.vsct.dt.strowgr.admin.core.event.in.CommitSuccessEvent;
import fr.vsct.dt.nsq.NSQMessage;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommitCompletedTransformerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private final CommitCompletedTransformer commitCompletedTransformer = new CommitCompletedTransformer(mapper);

    @Test
    public void should_transform_commit_completed_event_to_object() throws Exception {
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
        CommitSuccessEvent result = commitCompletedTransformer.apply(nsqMessage);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getCorrelationId()).isEqualTo("someCorrelationId");
        assertThat(result.getKey().getID()).isEqualTo("someApp/somePlatform");
    }
}