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

import com.vsct.dt.strowgr.admin.core.repository.EntryPointRepository;
import io.reactivex.Flowable;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EntryPointPublisherTest {

    private final EntryPointRepository repository = mock(EntryPointRepository.class);

    private final EntryPointPublisher<String> entryPointPublisher = new EntryPointPublisher<>(repository, String::toUpperCase);

    @Test
    public void should_build_flowable_with_mapped_list_of_entry_points_from_repository() throws Exception {
        // given
        when(repository.getEntryPointsId()).thenReturn(new HashSet<>(Arrays.asList("ep1", "ep2")));

        // when
        Flowable<String> flowable = entryPointPublisher.apply(0L);

        // then
        List<String> result = new ArrayList<>();
        flowable.subscribe(result::add);
        assertThat(result).containsExactlyInAnyOrder("EP1", "EP2");
    }

    @Test
    public void should_build_empty_flowable_when_repository_throws_exception() throws Exception {
        // given
        when(repository.getEntryPointsId()).thenThrow(new RuntimeException());

        // when
        Flowable<String> flowable = entryPointPublisher.apply(0L);

        // then
        List<String> result = new ArrayList<>();
        flowable.subscribe(result::add);
        assertThat(result).isEmpty();
    }
}