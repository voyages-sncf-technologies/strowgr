package com.vsct.dt.strowgr.admin.gui;

import com.github.brainlag.nsq.NSQProducer;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NSQ client manager by Dropwizard.
 */
class NSQProducerManager implements Managed {
    private static final Logger LOGGER = LoggerFactory.getLogger(NSQProducerManager.class);

    private final NSQProducer nsqProducer;

    NSQProducerManager(NSQProducer nsqProducer) {
        this.nsqProducer = nsqProducer;
    }

    @Override
    public void start() throws Exception {
        LOGGER.info("start NSQProducer");
        this.nsqProducer.start();
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("stop NSQProducer");
        this.nsqProducer.shutdown();
    }
}
