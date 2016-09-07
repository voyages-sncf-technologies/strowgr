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

package com.vsct.dt.strowgr.admin.scheduler;

import com.vsct.dt.strowgr.admin.core.EntryPointKeyDefaultImpl;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.core.repository.EntryPointRepository;
import com.vsct.dt.strowgr.admin.core.event.CorrelationId;
import com.vsct.dt.strowgr.admin.core.event.in.TryCommitCurrentConfigurationEvent;
import com.vsct.dt.strowgr.admin.core.event.in.TryCommitPendingConfigurationEvent;
import com.vsct.dt.strowgr.admin.core.repository.HaproxyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Schedule entrypoint lifecycles.
 * <p>
 * Created by william_montaz on 11/02/2016.
 */
public class PeriodicScheduler<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicScheduler.class);

    private final EntryPointRepository entryPointRepository;
    private HaproxyRepository haproxyRepository;
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
                    LOGGER.error("a sleep interruption", e);
                    return;
                }
                try {
                    Set<String> disabledHaproxy = haproxyRepository.getDisabledHaproxyIds().orElseThrow(() -> new RuntimeException("can't retrieve disabled haproxy, entry point configuration can't sent to sidekicks."));
                    for (String entryPointId : entryPointRepository.getEntryPointsId()) {
                        Optional<EntryPoint> currentConfiguration = entryPointRepository.getCurrentConfiguration(new EntryPointKeyDefaultImpl(entryPointId));
                        if (currentConfiguration.isPresent()) {
                            if( disabledHaproxy.contains(currentConfiguration.get().getHaproxy())){
                                // skip
                            } else {

                            }
                        }
                        consumer.accept(provider.apply(entryPointId));
                    }
                } catch (Throwable t) {
                    LOGGER.error("PeriodicScheduler failed.", t);
                }
            }
        }
    });

    public PeriodicScheduler(EntryPointRepository entryPointRepository, HaproxyRepository haproxyRepository, Function<String, T> provider, Consumer<T> consumer, long periodMilli) {
        this.entryPointRepository = entryPointRepository;
        this.haproxyRepository = haproxyRepository;
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

    public static PeriodicScheduler<TryCommitPendingConfigurationEvent> newPeriodicCommitPendingScheduler(EntryPointRepository repository, HaproxyRepository haproxyRepository, Consumer<TryCommitPendingConfigurationEvent> consumer, long period) {
        return new PeriodicScheduler<>(repository, haproxyRepository, ep -> new TryCommitPendingConfigurationEvent(CorrelationId.newCorrelationId(), new EntryPointKeyDefaultImpl(ep)), consumer, period);
    }

    public static PeriodicScheduler<TryCommitCurrentConfigurationEvent> newPeriodicCommitCurrentScheduler(EntryPointRepository entryPointRepository, Consumer<TryCommitCurrentConfigurationEvent> consumer, long period, HaproxyRepository haproxyRepository) {
        return new PeriodicScheduler<>(entryPointRepository, haproxyRepository, ep -> new TryCommitCurrentConfigurationEvent(CorrelationId.newCorrelationId(), new EntryPointKeyDefaultImpl(ep)), consumer, period);
    }

}
