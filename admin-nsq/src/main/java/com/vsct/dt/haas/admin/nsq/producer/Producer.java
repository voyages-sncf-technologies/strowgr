package com.vsct.dt.haas.admin.nsq.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQProducer;
import com.github.brainlag.nsq.exceptions.NSQException;

import java.util.Base64;
import java.util.concurrent.TimeoutException;

public class Producer {

    private final NSQProducer producer;

    private final ObjectMapper mapper = new ObjectMapper();

    public Producer(String host, int port) {
        this.producer = new NSQProducer();
        this.producer.addAddress(host, port);
    }

    public void sendCommitBegin(String correlationId, String haproxy, String application, String platform, String conf) throws JsonProcessingException, NSQException, TimeoutException {
        String confBase64 = new String(Base64.getEncoder().encode(conf.getBytes()));
        CommitBeginPayload payload = new CommitBeginPayload(correlationId, application, platform, confBase64);
        producer.produce("commit_requested_" + haproxy, mapper.writeValueAsBytes(payload));
    }

    public void start(){
        this.producer.start();
    }

    public void stop(){
        this.producer.shutdown();
    }

}
