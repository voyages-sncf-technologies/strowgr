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

import com.vsct.dt.strowgr.admin.core.repository.EntryPointRepository;
import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EntryPoint publisher.
 *
 * @param <T> type parameter of the configuration to commit ({@link com.vsct.dt.strowgr.admin.core.event.in.TryCommitPendingConfigurationEvent}, {@link com.vsct.dt.strowgr.admin.core.event.in.TryCommitCurrentConfigurationEvent})
 */
public class EntryPointPublisher<T> implements Function<Long, Flowable<T>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryPointPublisher.class);

    private final EntryPointRepository entryPointRepository;

    private final Function<String, T> provider;

    /**
     * Constructor
     *
     * @param entryPointRepository repository where to retrieve entry points
     * @param provider             function providing the entry point to forward
     */
    public EntryPointPublisher(EntryPointRepository entryPointRepository, Function<String, T> provider) {
        this.entryPointRepository = entryPointRepository;
        this.provider = provider;
    }

    @Override
    public Flowable<T> apply(Long n) {
        try {
            return Flowable.fromIterable(entryPointRepository.getEntryPointsId()).map(provider);
        } catch (Exception e) {
            LOGGER.error("Unable to lookup entry points.", e);
            return Flowable.empty();
        }
    }

}
