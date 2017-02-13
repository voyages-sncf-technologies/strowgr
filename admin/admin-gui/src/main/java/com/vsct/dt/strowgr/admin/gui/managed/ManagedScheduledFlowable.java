package com.vsct.dt.strowgr.admin.gui.managed;

import io.dropwizard.lifecycle.Managed;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.flowables.ConnectableFlowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ManagedScheduledFlowable implements Managed {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedScheduledFlowable.class);

    private final String name;

    private final ConnectableFlowable<Long> flowable;

    private Disposable disposable = null;

    public ManagedScheduledFlowable(String name, Long interval, TimeUnit intervalUnit, Scheduler scheduler) {
        this.name = name;
        this.flowable = Flowable.interval(interval, intervalUnit, scheduler).publish();
    }

    @Override
    public void start() throws Exception {
        LOGGER.info("Starting managed scheduler for: {}.", name);
        disposable = flowable.connect();
    }

    public Flowable<Long> getFlowable() {
        return flowable;
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping managed scheduler for: {}.", name);
        Optional.ofNullable(disposable).ifPresent(Disposable::dispose);
    }

}
