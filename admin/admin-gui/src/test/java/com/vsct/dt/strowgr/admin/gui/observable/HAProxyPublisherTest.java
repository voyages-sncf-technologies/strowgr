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
package com.vsct.dt.strowgr.admin.gui.observable;

import com.vsct.dt.strowgr.admin.gui.observable.HAProxyPublisher.HAProxyAction;
import com.vsct.dt.strowgr.admin.repository.consul.ConsulRepository;
import io.reactivex.Flowable;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HAProxyPublisherTest {

    private ConsulRepository repository = mock(ConsulRepository.class);

    private final HAProxyPublisher HAProxyPublisher = new HAProxyPublisher(repository);

    @Test
    public void should_publish_registration_actions_when_repository_returns_more_elements_than_previous_call() {
        // given
        when(repository.getHaproxyIds()).thenReturn(new HashSet<>(asList("hap1", "hap2")));

        // when
        Flowable<HAProxyAction> flowable1 = HAProxyPublisher.apply(0L);

        // then
        List<HAProxyAction> result1 = new ArrayList<>();
        flowable1.subscribe(result1::add);
        assertThat(result1).extracting(HAProxyAction::getId).containsExactlyInAnyOrder("hap1", "hap2");
        assertThat(result1).extracting(HAProxyAction::isRegistration).containsExactlyInAnyOrder(true, true);

        // given
        when(repository.getHaproxyIds()).thenReturn(new HashSet<>(asList("hap1", "hap2", "hap3", "hap4")));

        // when
        Flowable<HAProxyAction> flowable2 = HAProxyPublisher.apply(1L);

        // then
        List<HAProxyAction> result2 = new ArrayList<>();
        flowable2.subscribe(result2::add);
        assertThat(result2).extracting(HAProxyAction::getId).containsExactlyInAnyOrder("hap3", "hap4").doesNotContain("hap1", "hap2");
        assertThat(result2).extracting(HAProxyAction::isRegistration).containsExactlyInAnyOrder(true, true);
    }

    @Test
    public void should_publish_unregistration_actions_when_repository_returns_less_elements_than_previous_call() {
        // given
        when(repository.getHaproxyIds()).thenReturn(new HashSet<>(asList("hap1", "hap2", "hap3", "hap4")));

        // when
        Flowable<HAProxyAction> flowable1 = HAProxyPublisher.apply(0L);

        // then
        List<HAProxyAction> result1 = new ArrayList<>();
        flowable1.subscribe(result1::add);
        assertThat(result1).extracting(HAProxyAction::getId).containsExactlyInAnyOrder("hap1", "hap2", "hap3", "hap4");
        assertThat(result1).extracting(HAProxyAction::isRegistration).containsExactlyInAnyOrder(true, true, true, true);

        // given
        when(repository.getHaproxyIds()).thenReturn(new HashSet<>(asList("hap1", "hap2")));

        // when
        Flowable<HAProxyAction> flowable2 = HAProxyPublisher.apply(1L);

        // then
        List<HAProxyAction> result2 = new ArrayList<>();
        flowable2.subscribe(result2::add);
        assertThat(result2).extracting(HAProxyAction::getId).containsExactlyInAnyOrder("hap3", "hap4").doesNotContain("hap1", "hap2");
        assertThat(result2).extracting(HAProxyAction::isRegistration).containsExactlyInAnyOrder(false, false);
    }

    @Test
    public void should_publish_empty_actions_when_repository_throws_exception() {
        // given
        when(repository.getHaproxyIds()).thenThrow(new RuntimeException());

        // when
        Flowable<HAProxyAction> flowable = HAProxyPublisher.apply(0L);

        // then
        List<HAProxyAction> result = new ArrayList<>();
        flowable.subscribe(result::add);
        assertThat(result).isEmpty();
    }

}
