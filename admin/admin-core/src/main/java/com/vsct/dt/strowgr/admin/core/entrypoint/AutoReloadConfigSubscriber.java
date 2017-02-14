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
import io.reactivex.functions.Consumer;

public class AutoReloadConfigSubscriber implements Consumer<AutoReloadConfigEvent> {

    private final EntryPointStateManager stateManager;

    public AutoReloadConfigSubscriber(EntryPointStateManager stateManager) {
        this.stateManager = stateManager;
    }

    @Override
    public void accept(AutoReloadConfigEvent autoReloadConfigEvent) {

        EntryPointKey entryPointKey = autoReloadConfigEvent.getKey();
        try {

            if (!this.stateManager.lock(entryPointKey)) {
                throw new IllegalStateException("Lock could not be acquired when swapping auto reload config for " + entryPointKey);
            }

            boolean isAutoReloaded = this.stateManager.isAutoreloaded(entryPointKey);
            this.stateManager.setAutoreload(entryPointKey, !isAutoReloaded);
            autoReloadConfigEvent.onSuccess(new AutoReloadConfigResponse(autoReloadConfigEvent.getCorrelationId(), entryPointKey, true));

        } catch (Exception e) {
            autoReloadConfigEvent.onError(e);
        } finally {
            this.stateManager.release(entryPointKey);
        }

    }

}
