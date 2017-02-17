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
import com.vsct.dt.strowgr.admin.nsq.payload.CommitCompleted;
import fr.vsct.dt.nsq.NSQMessage;
import io.reactivex.functions.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommitCompletedTransformer implements Function<NSQMessage, CommitSuccessEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommitCompletedTransformer.class);

    private final ObjectMapper objectMapper;

    public CommitCompletedTransformer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public CommitSuccessEvent apply(NSQMessage nsqMessage) throws Exception {
        CommitCompleted payload = objectMapper.readValue(nsqMessage.getMessage(), CommitCompleted.class);

        LOGGER.debug("received an new CommitSuccesEvent with cid {}", payload.getHeader().getCorrelationId());

        return new CommitSuccessEvent(
                payload.getHeader().getCorrelationId(),
                new EntryPointKeyVsctImpl(
                        payload.getHeader().getApplication(),
                        payload.getHeader().getPlatform()
                )
        );
    }

}
