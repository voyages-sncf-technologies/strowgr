package com.vsct.dt.strowgr.admin.nsq.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQProducer;
import com.github.brainlag.nsq.exceptions.NSQException;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.concurrent.TimeoutException;

/**
 * Dispatcher of events to NSQ.
 *
 * WARNING: NSQProducer is not managed by this dispatcher. For instance the start/shutdown should be done outside this
 * object.
 */
public class NSQDispatcher {

    private final NSQProducer nsqProducer;

    private final ObjectMapper mapper = new ObjectMapper();

    public NSQDispatcher(NSQProducer nsqProducer) {
        this.nsqProducer = nsqProducer;
    }

    public void sendCommitRequested(String correlationId, String haproxy, String application, String platform, String conf, String syslogConf) throws JsonProcessingException, NSQException, TimeoutException, UnsupportedEncodingException {
        String confBase64 = new String(Base64.getEncoder().encode(conf.getBytes("UTF-8")));
        String syslogConfBase64 = new String(Base64.getEncoder().encode(syslogConf.getBytes("UTF-8")));
        CommitBeginPayload payload = new CommitBeginPayload(correlationId, application, platform, confBase64, syslogConfBase64);
        nsqProducer.produce("commit_requested_" + haproxy, mapper.writeValueAsBytes(payload));
    }

}
