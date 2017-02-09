package com.vsct.dt.strowgr.admin.gui.observable;

import com.vsct.dt.strowgr.admin.core.event.out.DeleteEntryPointEvent;
import com.vsct.dt.strowgr.admin.nsq.producer.NSQDispatcher;
import io.reactivex.functions.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteEntryPointSubscriber implements Consumer<DeleteEntryPointEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteEntryPointSubscriber.class);

    private final NSQDispatcher nsqDispatcher;

    public DeleteEntryPointSubscriber(NSQDispatcher nsqDispatcher) {
        this.nsqDispatcher = nsqDispatcher;
    }

    @Override
    public void accept(DeleteEntryPointEvent deleteEntryPointEvent) {

        try {
            this.nsqDispatcher.sendDeleteRequested(deleteEntryPointEvent.getCorrelationId(), deleteEntryPointEvent.getHaproxyName(), deleteEntryPointEvent.getApplication(), deleteEntryPointEvent.getPlatform());
        } catch (Exception e) {
            LOGGER.error("Unable to send delete entry point event {} to NSQ because of the following errors", deleteEntryPointEvent, e);
        }

    }

}
