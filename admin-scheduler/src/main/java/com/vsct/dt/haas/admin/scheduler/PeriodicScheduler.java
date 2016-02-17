package com.vsct.dt.haas.admin.scheduler;

import com.vsct.dt.haas.admin.core.EntryPointKeyDefaultImpl;
import com.vsct.dt.haas.admin.core.EntryPointRepository;
import com.vsct.dt.haas.admin.core.event.CorrelationId;
import com.vsct.dt.haas.admin.core.event.in.TryCommitCurrentConfigurationEvent;
import com.vsct.dt.haas.admin.core.event.in.TryCommitPendingConfigurationEvent;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by william_montaz on 11/02/2016.
 */
public class PeriodicScheduler<T> {

    private final EntryPointRepository repository;
    private final Consumer<T> consumer;
    private final Function<String, T> provider;
    private final long periodMilli;
    private volatile boolean stop = false;

    private final Thread automaticScheduler = new Thread(new Runnable() {
        @Override
        public void run() {
            while (!stop) {
                try {
                    Thread.sleep(periodMilli);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
                for (String ep : repository.getEntryPointsId()) {
                    consumer.accept(provider.apply(ep));
                }
            }
        }
    });

    public PeriodicScheduler(EntryPointRepository repository, Function<String, T> provider, Consumer<T> consumer, long periodMilli) {
        this.repository = repository;
        this.consumer = consumer;
        this.provider = provider;
        this.periodMilli = periodMilli;
    }

    public void start() {
        automaticScheduler.setDaemon(true);
        automaticScheduler.start();
    }

    public void stop() {
        stop = true;
    }

    public static PeriodicScheduler<TryCommitPendingConfigurationEvent> newPeriodicCommitPendingScheduler(EntryPointRepository repository, Consumer<TryCommitPendingConfigurationEvent> consumer, long period) {
        return new PeriodicScheduler<>(repository, ep -> {
            return new TryCommitPendingConfigurationEvent(CorrelationId.newCorrelationId(), new EntryPointKeyDefaultImpl(ep));
        }, consumer, period);
    }

    public static PeriodicScheduler<TryCommitCurrentConfigurationEvent> newPeriodicCommitCurrentScheduler(EntryPointRepository repository, Consumer<TryCommitCurrentConfigurationEvent> consumer, long period) {
        return new PeriodicScheduler<>(repository, ep -> {
            return new TryCommitCurrentConfigurationEvent(CorrelationId.newCorrelationId(), new EntryPointKeyDefaultImpl(ep));
        }, consumer, period);
    }

}
