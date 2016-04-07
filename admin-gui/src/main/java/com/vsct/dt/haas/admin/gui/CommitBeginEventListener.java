package com.vsct.dt.haas.admin.gui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.brainlag.nsq.exceptions.NSQException;
import com.google.common.eventbus.Subscribe;
import com.vsct.dt.haas.admin.core.configuration.EntryPointConfiguration;
import com.vsct.dt.haas.admin.core.event.out.CommitBeginEvent;
import com.vsct.dt.haas.admin.nsq.producer.Producer;

import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Listen CommitBeginEvent.
 *
 * Created by william_montaz on 15/02/2016.
 */
public class CommitBeginEventListener {

    private final Producer producer;

    public CommitBeginEventListener(Producer producer) {
        this.producer = producer;
    }

    @Subscribe
    public void handle(CommitBeginEvent commitBeginEvent) throws NSQException, TimeoutException, JsonProcessingException {
        EntryPointConfiguration configuration = commitBeginEvent.getConfiguration();
        Map<String, String> context = configuration.getContext();
        String application = context.get("application");
        String platform = context.get("platform");
        /* TODO test application and platform nullity */
        this.producer.sendCommitRequested(commitBeginEvent.getCorrelationId(), configuration.getHaproxy(), application, platform, commitBeginEvent.getConf(), commitBeginEvent.getSyslogConf());
    }
}
