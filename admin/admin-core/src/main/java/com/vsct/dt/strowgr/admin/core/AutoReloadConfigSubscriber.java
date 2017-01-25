package com.vsct.dt.strowgr.admin.core;

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
