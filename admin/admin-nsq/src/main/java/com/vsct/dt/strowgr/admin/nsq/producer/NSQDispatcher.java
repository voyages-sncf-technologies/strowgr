/*
 * Copyright (C) 2016 VSCT
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vsct.dt.strowgr.admin.nsq.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsct.dt.nsq.NSQProducer;
import com.vsct.dt.nsq.exceptions.NSQException;
import com.vsct.dt.strowgr.admin.core.event.out.CommitRequestedEvent;
import com.vsct.dt.strowgr.admin.nsq.payload.CommitRequested;
import com.vsct.dt.strowgr.admin.nsq.payload.DeleteRequested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.concurrent.TimeoutException;

/**
 * Dispatcher of events to NSQ.
 * <p>
 * WARNING: NSQProducer is not managed by this dispatcher. For instance the start/shutdown should be done outside this
 * object.
 */
public class NSQDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(NSQDispatcher.class);

    private final NSQProducer nsqProducer;

    private final ObjectMapper mapper = new ObjectMapper();

    public NSQDispatcher(NSQProducer nsqProducer) {
        this.nsqProducer = nsqProducer;
    }

    /**
     * Send a {@link CommitRequested} message to commit_requested_[haproxyName] NSQ topic.
     *
     * @param commitRequestedEvent in commit requested event
     * @param haproxyName          name of the targeted entrypoint
     * @param application          of the targeted entrypoint
     * @param platform             of the targeted entrypoint
     * @param bind
     * @throws JsonProcessingException      during a Json serialization with Jackson
     * @throws NSQException                 during any problem with NSQ
     * @throws TimeoutException             during a too long response from NSQ
     * @throws UnsupportedEncodingException during the conversion to UTF-8
     */
    public void sendCommitRequested(CommitRequestedEvent commitRequestedEvent, String haproxyName, String application, String platform, String bind) throws JsonProcessingException, NSQException, TimeoutException, UnsupportedEncodingException {
        String confBase64 = new String(Base64.getEncoder().encode(commitRequestedEvent.getConf().getBytes("UTF-8")));
        String syslogConfBase64 = new String(Base64.getEncoder().encode(commitRequestedEvent.getSyslogConf().getBytes("UTF-8")));
        CommitRequested payload = new CommitRequested(commitRequestedEvent.getCorrelationId(), application, platform, confBase64, syslogConfBase64, commitRequestedEvent.getConfiguration().getHapVersion(), bind);

        try {
            nsqProducer.produce("commit_requested_" + haproxyName, mapper.writeValueAsBytes(payload));
        } catch (NSQException | TimeoutException | JsonProcessingException e) {
            LOGGER.error("can't produce NSQ message to commit_requested_" + haproxyName, e);
        }
    }

    /**
     * Send a {@link DeleteRequested} message to delete_requested_[haproxyName] NSQ topic.
     *
     * @param correlationId from initial request
     * @param haproxyName   name of the targeted entrypoint
     * @param application   of the targeted entrypoint
     * @param platform      of the targeted entrypoint
     * @throws JsonProcessingException during a Json serialization with Jackson
     * @throws NSQException            during any problem with NSQ
     * @throws TimeoutException        during a too long response from NSQ
     */
    public void sendDeleteRequested(String correlationId, String haproxyName, String application, String platform) throws JsonProcessingException, NSQException, TimeoutException {
        DeleteRequested deleteRequestedPayload = new DeleteRequested(correlationId, application, platform);
        nsqProducer.produce("delete_requested_" + haproxyName, mapper.writeValueAsBytes(deleteRequestedPayload));
    }
}
