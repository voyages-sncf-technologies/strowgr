package com.vsct.dt.strowgr.admin.gui.subscribers;

import com.google.common.eventbus.EventBus;
import io.reactivex.subscribers.DefaultSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;

/**
 * ~  Copyright (C) 2016 VSCT
 * ~
 * ~  Licensed under the Apache License, Version 2.0 (the "License");
 * ~  you may not use this file except in compliance with the License.
 * ~  You may obtain a copy of the License at
 * ~
 * ~   http://www.apache.org/licenses/LICENSE-2.0
 * ~
 * ~  Unless required by applicable law or agreed to in writing, software
 * ~  distributed under the License is distributed on an "AS IS" BASIS,
 * ~  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * ~  See the License for the specific language governing permissions and
 * ~  limitations under the License.
 * ~
 * <p>
 * THIS CLASS IS TEMPORARY AND WILL BE REMOVED WHEN WE WILL GET RID OF THE EVENT BUS
 */
public class EventBusSubscriber extends DefaultSubscriber<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventBusSubscriber.class);

    private final EventBus eventBus;
    private final BlockingQueue eventBusQueue;

    private int remaining = 0;

    public EventBusSubscriber(EventBus eventBus, BlockingQueue eventBusQueue) {
        this.eventBus = eventBus;
        this.eventBusQueue = eventBusQueue;
    }

    @Override
    public void onStart() {
        //start slow
        request(1);
        remaining++;
    }

    @Override
    public void onComplete() {
        //Nothing to do
        LOGGER.debug("EventSubscriber got onCompleted message");
    }

    @Override
    public void onError(Throwable e) {
        LOGGER.error("An unexpected error happened while observing nsq events", e);
    }

    @Override
    public void onNext(Object event) {
        boolean published = false;

        //block with spinning when not able to publish
        while (!published) {
            try {
                eventBus.post(event);
                published = true;
            } catch (RejectedExecutionException e) {
                LOGGER.debug("EventBus rejected execution, loop until it accepts it !");
            }
        }

        remaining--;

        LOGGER.debug("EventBusSubscriber can still request {} events", remaining);

        if (remaining == 0) {
            //make sure to request at least one message
            remaining = Math.max(1, eventBusQueue.remainingCapacity());
            LOGGER.debug("request {} new messages", remaining);
            request(remaining);
        }
    }

}
