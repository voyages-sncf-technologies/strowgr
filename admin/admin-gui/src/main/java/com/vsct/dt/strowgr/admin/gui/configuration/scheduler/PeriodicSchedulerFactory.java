/*
 * Copyright (C) 2016 VSCT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vsct.dt.strowgr.admin.gui.configuration.scheduler;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.strowgr.admin.gui.configuration.scheduler.PeriodicCommitCurrentSchedulerFactory;
import com.vsct.dt.strowgr.admin.gui.configuration.scheduler.PeriodicCommitPendingSchedulerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by william_montaz on 16/02/2016.
 */
public class PeriodicSchedulerFactory {

    @Valid
    @NotNull
    private PeriodicCommitCurrentSchedulerFactory periodicCommitCurrentSchedulerFactory;

    @Valid
    @NotNull
    private PeriodicCommitPendingSchedulerFactory periodicCommitPendingSchedulerFactory;

    @JsonProperty("current")
    public PeriodicCommitCurrentSchedulerFactory getPeriodicCommitCurrentSchedulerFactory() {
        return periodicCommitCurrentSchedulerFactory;
    }

    @JsonProperty("current")
    public void setPeriodicCommitCurrentSchedulerFactory(PeriodicCommitCurrentSchedulerFactory periodicCommitCurrentSchedulerFactory) {
        this.periodicCommitCurrentSchedulerFactory = periodicCommitCurrentSchedulerFactory;
    }

    @JsonProperty("pending")
    public PeriodicCommitPendingSchedulerFactory getPeriodicCommitPendingSchedulerFactory() {
        return periodicCommitPendingSchedulerFactory;
    }

    @JsonProperty("pending")
    public void setPeriodicCommitPendingSchedulerFactory(PeriodicCommitPendingSchedulerFactory periodicCommitPendingSchedulerFactory) {
        this.periodicCommitPendingSchedulerFactory = periodicCommitPendingSchedulerFactory;
    }
}
