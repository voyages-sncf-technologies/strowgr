package com.vsct.dt.strowgr.admin.core.event.in;

import com.vsct.dt.strowgr.admin.core.EntryPointKey;

/**
 * Created by william_montaz on 05/02/2016.
 */
public class CommitSuccessEvent extends EntryPointEvent {

    public CommitSuccessEvent(String correlationId, EntryPointKey key) {
        super(correlationId, key);
    }

}
