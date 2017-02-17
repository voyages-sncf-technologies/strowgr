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
package com.vsct.dt.strowgr.admin.nsq.producer;

import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.core.event.out.CommitRequestedEvent;
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
                nsqDispatcher.sendCommitRequested(commitRequestedEvent, configuration.getHaproxy(), application, platform, commitRequestedEvent.getBind());
            } catch (Exception e) {
                LOGGER.error("Unable to send commit requested event {} to NSQ because of the following errors", commitRequestedEvent, e);
            }

        }));

    }

}
