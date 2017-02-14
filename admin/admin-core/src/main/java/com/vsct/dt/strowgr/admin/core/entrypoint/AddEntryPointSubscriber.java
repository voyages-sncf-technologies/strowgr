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
package com.vsct.dt.strowgr.admin.core.entrypoint;

import com.vsct.dt.strowgr.admin.core.EntryPointKey;
import com.vsct.dt.strowgr.admin.core.EntryPointStateManager;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.core.repository.HaproxyRepository;
import io.reactivex.functions.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class AddEntryPointSubscriber implements Consumer<AddEntryPointEvent> {

    private final static Logger LOGGER = LoggerFactory.getLogger(AddEntryPointSubscriber.class);

    private final EntryPointStateManager stateManager;

    private final HaproxyRepository haproxyRepository;

    public AddEntryPointSubscriber(EntryPointStateManager stateManager, HaproxyRepository haproxyRepository) {
        this.stateManager = stateManager;
        this.haproxyRepository = haproxyRepository;
    }

    @Override
    public void accept(AddEntryPointEvent addEntryPointEvent) {

        LOGGER.debug("handles AddEntryPointEvent");
        EntryPointKey entryPointKey = addEntryPointEvent.getKey();
        try {

            EntryPoint entryPoint = addEntryPointEvent.getConfiguration();

            // force auto reload of entry point even if lock will fail
            if (this.haproxyRepository.getHaproxyProperty(entryPoint.getHaproxy(), "platform").orElse("").equals("production")) {
                this.stateManager.setAutoreload(entryPointKey, false);
            } else {
                this.stateManager.setAutoreload(entryPointKey, true);
            }

            if (!this.stateManager.lock(entryPointKey)) {
                throw new IllegalStateException("Lock could not be acquired while adding entry point " + entryPointKey);
            }

            if (stateManager.getCommittingConfiguration(entryPointKey).isPresent() || stateManager.getCurrentConfiguration(entryPointKey).isPresent()) {
                throw new IllegalStateException("Entry Point configuration already present " + entryPointKey);
            }

            Optional<EntryPoint> preparedConfiguration = stateManager.prepare(entryPointKey, entryPoint);
            if (!preparedConfiguration.isPresent()) {
                throw new IllegalStateException("Entry point for key " + entryPointKey + " could not be created");
            }

            AddEntryPointResponse addEntryPointResponse = new AddEntryPointResponse(addEntryPointEvent.getCorrelationId(), entryPointKey, preparedConfiguration.get());
            LOGGER.trace("from handle AddEntryPointEvent new EntryPoint {} added -> {}", entryPointKey.getID(), addEntryPointResponse);
            addEntryPointEvent.onSuccess(addEntryPointResponse);

        } catch (Exception exception) {
            addEntryPointEvent.onError(exception);
        } finally {
            this.stateManager.release(entryPointKey);
        }
    }

}
