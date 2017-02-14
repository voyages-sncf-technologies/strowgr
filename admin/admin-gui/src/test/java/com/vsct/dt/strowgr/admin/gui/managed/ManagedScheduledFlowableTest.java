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

import io.reactivex.schedulers.TestScheduler;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class ManagedScheduledFlowableTest {

    private final TestScheduler testScheduler = new TestScheduler();

    private final ManagedScheduledFlowable managedScheduledFlowable = new ManagedScheduledFlowable("test", 1L, TimeUnit.SECONDS, testScheduler);

    @Test
    public void should_start_item_emission_when_started() throws Exception {
        // given
        List<Long> results = new ArrayList<>();
        managedScheduledFlowable.getFlowable().subscribe(results::add);
        managedScheduledFlowable.start();

        // when
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        // then
        assertThat(results).containsExactly(0L, 1L);
    }

    @Test
    public void should_not_start_item_emission_when_not_started() throws Exception {
        // given
        List<Long> results = new ArrayList<>();
        managedScheduledFlowable.getFlowable().subscribe(results::add);

        // when
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        // then
        assertThat(results).isEmpty();
    }

    @Test
    public void should_stop_item_emission_when_stopped() throws Exception {
        // given
        List<Long> results = new ArrayList<>();
        managedScheduledFlowable.getFlowable().subscribe(results::add);
        managedScheduledFlowable.start();

        // when
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        managedScheduledFlowable.stop();
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        // then
        assertThat(results).containsExactly(0L, 1L).doesNotContain(2L, 3L);
    }
}