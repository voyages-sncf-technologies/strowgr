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

package com.vsct.dt.strowgr.admin.gui.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.strowgr.admin.core.repository.EntryPointRepository;
import com.vsct.dt.strowgr.admin.core.event.in.TryCommitPendingConfigurationEvent;
import com.vsct.dt.strowgr.admin.scheduler.PeriodicScheduler;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.Min;
import java.util.function.Consumer;

/**
 * Created by william_montaz on 16/02/2016.
 */
public class PeriodicCommitPendingSchedulerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicCommitPendingSchedulerFactory.class);

    @Min(1)
    private long periodMilli;

    @JsonProperty("periodMilli")
    public long getPeriodMilli() {
        return periodMilli;
    }

    @JsonProperty("periodMilli")
    public void setPeriodMilli(long periodMilli) {
        this.periodMilli = periodMilli;
    }

    public PeriodicScheduler build(EntryPointRepository entryPointRepository, Consumer<TryCommitPendingConfigurationEvent> consumer, Environment environment) {
        PeriodicScheduler scheduler = PeriodicScheduler.newPeriodicCommitPendingScheduler(entryPointRepository, consumer, getPeriodMilli());
        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() throws Exception {
                LOGGER.info("Starting CommitPendingScheduler");
                scheduler.start();
            }

            @Override
            public void stop() throws Exception {
                LOGGER.info("Stopping CommitPendingScheduler");
                scheduler.stop();
            }
        });
        return scheduler;
    }
}
