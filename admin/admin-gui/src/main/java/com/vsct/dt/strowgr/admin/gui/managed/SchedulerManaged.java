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

package com.vsct.dt.strowgr.admin.gui.managed;

import com.vsct.dt.strowgr.admin.core.repository.EntryPointRepository;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class SchedulerManaged<T> implements Managed {
    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerManaged.class);

    private final String name; // mainly for logging
    private final ScheduledExecutorService scheduledExecutorService;
    private final EntryPointRepository entryPointRepository;
    private final Consumer<T> consumer;
    private final Function<String, T> provider;
    private final long periodMilli;

    public SchedulerManaged(String name, EntryPointRepository entryPointRepository, Function<String, T> provider, Consumer<T> consumer, long periodMilli) {
        this.name = name;
        this.entryPointRepository = entryPointRepository;
        this.consumer = consumer;
        this.provider = provider;
        this.periodMilli = periodMilli;
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
    }

    @Override
    public void start() throws Exception {
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
                , 0, periodMilli, TimeUnit.MILLISECONDS);
        LOGGER.info("Start scheduler for {} with period of {}ms", name, periodMilli);
    }

    @Override
    public void stop() throws Exception {
        scheduledExecutorService.shutdown();
        LOGGER.info("Shutdown scheduler {}", name);
    }


}
