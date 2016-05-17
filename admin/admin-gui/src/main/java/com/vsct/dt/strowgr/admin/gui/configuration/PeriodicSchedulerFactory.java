package com.vsct.dt.strowgr.admin.gui.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

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
