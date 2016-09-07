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

package com.vsct.dt.strowgr.admin.gui.configuration.scheduler;

import com.vsct.dt.strowgr.admin.core.EntryPointKeyDefaultImpl;
import com.vsct.dt.strowgr.admin.core.event.CorrelationId;
import com.vsct.dt.strowgr.admin.core.event.in.TryCommitCurrentConfigurationEvent;
import com.vsct.dt.strowgr.admin.core.event.in.TryCommitPendingConfigurationEvent;
import com.vsct.dt.strowgr.admin.core.repository.EntryPointRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Schedule entrypoint lifecycles.
 * <p>
 * Created by william_montaz on 11/02/2016.
 */
public class PeriodicScheduler<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicScheduler.class);

    private final ScheduledExecutorService scheduledExecutorService;
    private final EntryPointRepository entryPointRepository;
    private final Consumer<T> consumer;
    private final Function<String, T> provider;
    private final long periodMilli;

    public PeriodicScheduler(EntryPointRepository entryPointRepository, Function<String, T> provider, Consumer<T> consumer, long periodMilli) {
        this.entryPointRepository = entryPointRepository;
        this.consumer = consumer;
        this.provider = provider;
        this.periodMilli = periodMilli;
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
    }

    public void start() {
        scheduledExecutorService.scheduleAtFixedRate(
                () -> {
                    try {
                        for (String ep : entryPointRepository.getEntryPointsId()) {
                            consumer.accept(provider.apply(ep));
                        }
                    } catch (Throwable t) {
                        LOGGER.error("PeriodicScheduler failed.", t);
                    }
                }
                , 0, periodMilli, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduledExecutorService.shutdown();
    }

    public static PeriodicScheduler<TryCommitPendingConfigurationEvent> newPeriodicCommitPendingScheduler(EntryPointRepository repository, Consumer<TryCommitPendingConfigurationEvent> consumer, long period) {
        return new PeriodicScheduler<>(repository, ep -> new TryCommitPendingConfigurationEvent(CorrelationId.newCorrelationId(), new EntryPointKeyDefaultImpl(ep)), consumer, period);
    }

    public static PeriodicScheduler<TryCommitCurrentConfigurationEvent> newPeriodicCommitCurrentScheduler(EntryPointRepository entryPointRepository, Consumer<TryCommitCurrentConfigurationEvent> consumer, long period) {
        return new PeriodicScheduler<>(entryPointRepository, ep -> new TryCommitCurrentConfigurationEvent(CorrelationId.newCorrelationId(), new EntryPointKeyDefaultImpl(ep)), consumer, period);
    }

}
