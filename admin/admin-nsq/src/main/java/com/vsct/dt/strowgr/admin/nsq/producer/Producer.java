package com.vsct.dt.strowgr.admin.nsq.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQProducer;
import com.github.brainlag.nsq.exceptions.NSQException;
import com.vsct.dt.strowgr.admin.nsq.payload.CommitRequested;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.concurrent.TimeoutException;

public class Producer {

    private static final String SOURCE_NAME = "admin";

    private final String commitRequestedTopicPrefix;
    private final NSQProducer producer;

    private final ObjectMapper mapper = new ObjectMapper();

    public Producer(String host, int port, String commitRequestedTopicPrefix) {
        this.commitRequestedTopicPrefix = commitRequestedTopicPrefix;
        this.producer = new NSQProducer();
        this.producer.addAddress(host, port);
    }

    public void sendCommitRequested(String correlationId, String haproxy, String application, String platform, String haproxyConf, String syslogConf) throws JsonProcessingException, NSQException, TimeoutException, UnsupportedEncodingException {
        Base64.Encoder encoder = Base64.getEncoder();
        String confBase64 = new String(encoder.encode(haproxyConf.getBytes("UTF-8")));
        String syslogConfBase64 = new String(encoder.encode(syslogConf.getBytes("UTF-8")));

        CommitRequested payload = new CommitRequested(correlationId, application, platform, confBase64, syslogConfBase64);
        payload.getHeader().setSource(SOURCE_NAME);
        producer.produce(commitRequestedTopicPrefix + haproxy, mapper.writeValueAsBytes(payload));
    }

    public void start() {
        this.producer.start();
    }

    public void stop() {
        this.producer.shutdown();
    }

}
