package com.vsct.dt.strowgr.admin.gui;

import com.github.brainlag.nsq.NSQConsumer;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NSQ client manager by Dropwizard.
 */
class NSQConsumerManager implements Managed {
    private static final Logger LOGGER = LoggerFactory.getLogger(NSQConsumerManager.class);

    private final NSQConsumer nsqConsumer;

    NSQConsumerManager(NSQConsumer nsqConsumer) {
        this.nsqConsumer = nsqConsumer;
    }

    @Override
    public void start() throws Exception {
        LOGGER.info("start NSQConsumer {}");
        this.nsqConsumer.start();
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("stop NSQConsumer");
        this.nsqConsumer.shutdown();
    }
}
