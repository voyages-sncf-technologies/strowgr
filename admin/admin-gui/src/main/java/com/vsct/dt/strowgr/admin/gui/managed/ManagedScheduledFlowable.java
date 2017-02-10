package com.vsct.dt.strowgr.admin.gui.managed;

import io.dropwizard.lifecycle.Managed;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.flowables.ConnectableFlowable;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ManagedScheduledFlowable implements Managed {

    private final ConnectableFlowable<Long> flowable;

    private Disposable disposable = null;

    public ManagedScheduledFlowable(Long interval, TimeUnit intervalUnit, Scheduler scheduler) {
        this.flowable = Flowable.interval(interval, intervalUnit, scheduler).publish();
    }

    @Override
    public void start() throws Exception {
        disposable = flowable.connect();
    }

    public Flowable<Long> getFlowable() {
        return flowable;
    }

    @Override
    public void stop() throws Exception {
        Optional.ofNullable(disposable).ifPresent(Disposable::dispose);
    }

}
