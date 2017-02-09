package com.vsct.dt.strowgr.admin.gui.observable;

import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.core.event.out.CommitRequestedEvent;
import com.vsct.dt.strowgr.admin.nsq.producer.NSQDispatcher;
import io.reactivex.functions.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

public class CommitRequestedSubscriber implements Consumer<CommitRequestedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommitRequestedSubscriber.class);

    private final NSQDispatcher nsqDispatcher;

    public CommitRequestedSubscriber(NSQDispatcher nsqDispatcher) {
        this.nsqDispatcher = nsqDispatcher;
    }

    @Override
    public void accept(CommitRequestedEvent commitRequestedEvent) {

        EntryPoint configuration = commitRequestedEvent.getConfiguration();
        Map<String, String> context = configuration.getContext();

        Optional.ofNullable(context.get("application")).ifPresent(application -> Optional.ofNullable(context.get("platform")).ifPresent(platform -> {

            try {
                this.nsqDispatcher.sendCommitRequested(commitRequestedEvent, configuration.getHaproxy(), application, platform, commitRequestedEvent.getBind());
            } catch (Exception e) {
                LOGGER.error("Unable to send commit requested event {} to NSQ because of the following errors", commitRequestedEvent, e);
            }

        }));

    }

}
