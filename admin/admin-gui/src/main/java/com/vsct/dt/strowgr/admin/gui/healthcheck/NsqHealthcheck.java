package com.vsct.dt.strowgr.admin.gui.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import com.vsct.dt.strowgr.admin.nsq.producer.NSQHttpClient;

/**
 * Healthcheck on NSQ daemon which receive message from admin.
 */
public class NsqHealthcheck extends HealthCheck {

    private final NSQHttpClient nsqHttpClient;

    public NsqHealthcheck(NSQHttpClient nsqHttpClient) {
        this.nsqHttpClient = nsqHttpClient;
    }


    @Override
    protected Result check() throws Exception {
        Result healthCheck;
        if (nsqHttpClient.ping()) {
            healthCheck = Result.healthy();
        } else {
            healthCheck = Result.unhealthy("ping to NSQ has failed");
        }
        return healthCheck;
    }
}
