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
import io.reactivex.functions.Consumer;

import java.util.Optional;

public class UpdateEntryPointSubscriber implements Consumer<UpdateEntryPointEvent> {

    private final EntryPointStateManager stateManager;

    public UpdateEntryPointSubscriber(EntryPointStateManager stateManager) {
        this.stateManager = stateManager;
    }

    @Override
    public void accept(UpdateEntryPointEvent updateEntryPointEvent) {

        EntryPointKey entryPointKey = updateEntryPointEvent.getKey();

        try {

            if (!this.stateManager.lock(entryPointKey)) {
                throw new IllegalStateException("Could not acquire lock for update entry point event " + entryPointKey);
            }

            EntryPoint existingEntryPoint = Optional.ofNullable(stateManager.getPendingConfiguration(entryPointKey)
                    .orElseGet(() -> stateManager.getCommittingConfiguration(entryPointKey)
                            .orElseGet(() -> stateManager.getCurrentConfiguration(entryPointKey)
                                    .orElse(null))))
                    .orElseThrow(() -> new IllegalStateException("Trying to update a missing entry point for key " + entryPointKey));

            EntryPoint mergedEntryPoint = existingEntryPoint.mergeWithUpdate(updateEntryPointEvent.getUpdatedEntryPoint());

            Optional<EntryPoint> preparedEntryPoint = stateManager.prepare(entryPointKey, mergedEntryPoint);
            if (!preparedEntryPoint.isPresent()) {
                throw new IllegalStateException("Unable to prepare entry point update for key " + entryPointKey);
            }

            updateEntryPointEvent.onSuccess(new UpdateEntryPointResponse(updateEntryPointEvent.getCorrelationId(), entryPointKey, preparedEntryPoint.get()));

        } catch (Exception exception) {
            updateEntryPointEvent.onError(exception);
        } finally {
            this.stateManager.release(entryPointKey);
        }

    }

}
