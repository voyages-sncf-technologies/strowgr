package com.vsct.dt.haas.admin.gui.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.admin.core.EntryPointRepository;
import com.vsct.dt.haas.admin.core.event.in.TryCommitCurrentConfigurationEvent;
import com.vsct.dt.haas.admin.core.event.in.TryCommitPendingConfigurationEvent;
import com.vsct.dt.haas.admin.scheduler.PeriodicScheduler;
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

    public PeriodicScheduler build(EntryPointRepository repository, Consumer<TryCommitPendingConfigurationEvent> consumer, Environment environment) {
        PeriodicScheduler scheduler = PeriodicScheduler.newPeriodicCommitPendingScheduler(repository, consumer, getPeriodMilli());
        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() throws Exception {
                LOGGER.info("Starting CommitCurrentScheduler");
                scheduler.start();
            }

            @Override
            public void stop() throws Exception {
                LOGGER.info("Stopping CommitCurrentScheduler");
                scheduler.stop();
            }
        });
        return scheduler;
    }
}
