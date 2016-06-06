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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQProducer;
import com.github.brainlag.nsq.exceptions.NSQException;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.concurrent.TimeoutException;

public class Producer {

    public final String commitRequestedTopicPrefix;
    private final NSQProducer producer;

    private final ObjectMapper mapper = new ObjectMapper();

    public Producer(String host, int port, String commitRequestedTopicPrefix) {
        this.commitRequestedTopicPrefix = commitRequestedTopicPrefix;
        this.producer = new NSQProducer();
        this.producer.addAddress(host, port);
    }

    public void sendCommitRequested(String correlationId, String haproxy, String application, String platform, String conf, String syslogConf) throws JsonProcessingException, NSQException, TimeoutException, UnsupportedEncodingException {
        String confBase64 = new String(Base64.getEncoder().encode(conf.getBytes("UTF-8")));
        String syslogConfBase64 = new String(Base64.getEncoder().encode(syslogConf.getBytes("UTF-8")));
        CommitBeginPayload payload = new CommitBeginPayload(correlationId, application, platform, confBase64, syslogConfBase64);
        producer.produce(commitRequestedTopicPrefix + haproxy, mapper.writeValueAsBytes(payload));
    }

    public void start() {
        this.producer.start();
    }

    public void stop() {
        this.producer.shutdown();
    }

}
