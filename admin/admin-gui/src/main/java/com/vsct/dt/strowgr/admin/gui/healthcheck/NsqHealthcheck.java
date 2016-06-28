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
