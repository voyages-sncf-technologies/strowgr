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
 * <p>
 * WARNING: NSQProducer is not managed by this dispatcher. For instance the start/shutdown should be done outside this
 * object.
 */
public class NSQDispatcher {

    private final NSQProducer nsqProducer;

    private final ObjectMapper mapper = new ObjectMapper();

    public NSQDispatcher(NSQProducer nsqProducer) {
        this.nsqProducer = nsqProducer;
    }

    /**
     * Send a {@link CommitBeginPayload} message to commit_requested_[haproxyName] NSQ topic.
     *
     * @param correlationId from initial request
     * @param haproxyName   name of the targeted entrypoint
     * @param application   of the targeted entrypoint
     * @param platform      of the targeted entrypoint
     * @param haproxyConf   is the haproxy configuration content which is computed from the template and different registered values
     * @param syslogConf    is the haproxy configuration content which is computed from the template and different registered values
     * @throws JsonProcessingException      during a Json serialization with Jackson
     * @throws NSQException                 during any problem with NSQ
     * @throws TimeoutException             during a too long response from NSQ
     * @throws UnsupportedEncodingException during the conversion to UTF-8
     */
    public void sendCommitRequested(String correlationId, String haproxyName, String application, String platform, String haproxyConf, String syslogConf) throws JsonProcessingException, NSQException, TimeoutException, UnsupportedEncodingException {
        String confBase64 = new String(Base64.getEncoder().encode(haproxyConf.getBytes("UTF-8")));
        String syslogConfBase64 = new String(Base64.getEncoder().encode(syslogConf.getBytes("UTF-8")));
        CommitBeginPayload payload = new CommitBeginPayload(correlationId, application, platform, confBase64, syslogConfBase64);
        nsqProducer.produce("commit_requested_" + haproxyName, mapper.writeValueAsBytes(payload));
    }

    /**
     * Send a {@link DeleteRequestedPayload} message to delete_requested_[haproxyName] NSQ topic.
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
        DeleteRequestedPayload deleteRequestedPayload = new DeleteRequestedPayload(correlationId, application, platform, "admin"); // TODO find a better source information than 'admin'
        nsqProducer.produce("delete_requested_" + haproxyName, mapper.writeValueAsBytes(deleteRequestedPayload));
    }
}
