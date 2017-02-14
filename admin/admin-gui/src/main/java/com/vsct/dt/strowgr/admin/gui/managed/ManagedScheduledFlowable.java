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
