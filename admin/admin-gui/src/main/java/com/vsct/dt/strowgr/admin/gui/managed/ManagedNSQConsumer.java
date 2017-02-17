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

import com.vsct.dt.strowgr.admin.nsq.consumer.FlowableNSQConsumer;
import io.dropwizard.lifecycle.Managed;

import java.util.Objects;

public class ManagedNSQConsumer implements Managed {

    private final FlowableNSQConsumer<?> flowableNSQConsumer;

    public ManagedNSQConsumer(FlowableNSQConsumer<?> flowableNSQConsumer) {
        this.flowableNSQConsumer = Objects.requireNonNull(flowableNSQConsumer);
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {
        flowableNSQConsumer.shutdown();
    }

}
