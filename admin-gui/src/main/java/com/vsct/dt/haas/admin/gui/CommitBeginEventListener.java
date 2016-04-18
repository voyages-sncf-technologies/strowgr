package com.vsct.dt.haas.admin.gui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.brainlag.nsq.exceptions.NSQException;
import com.google.common.eventbus.Subscribe;
import com.vsct.dt.haas.admin.core.configuration.EntryPoint;
import com.vsct.dt.haas.admin.core.event.out.CommitBeginEvent;
import com.vsct.dt.haas.admin.nsq.producer.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Listen CommitBeginEvent.
 * <p/>
 * Created by william_montaz on 15/02/2016.
 */
public class CommitBeginEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommitBeginEventListener.class);

    private final Producer producer;

    public CommitBeginEventListener(Producer producer) {
        this.producer = producer;
    }

    @Subscribe
    public void handle(CommitBeginEvent commitBeginEvent) throws NSQException, TimeoutException, JsonProcessingException {
        EntryPoint configuration = commitBeginEvent.getConfiguration().orElseThrow(() -> new IllegalStateException("can't retrieve configuration of event " + commitBeginEvent));
        Map<String, String> context = configuration.getContext();
        String application = context.get("application");
        String platform = context.get("platform");
        /* TODO test application and platform nullity */
        LOGGER.debug("send to nsq a CommitRequested from CommitBeginEvent {}", commitBeginEvent);
        this.producer.sendCommitRequested(commitBeginEvent.getCorrelationId(), configuration.getHaproxy(), application, platform, commitBeginEvent.getConf(), commitBeginEvent.getSyslogConf());
    }
}
