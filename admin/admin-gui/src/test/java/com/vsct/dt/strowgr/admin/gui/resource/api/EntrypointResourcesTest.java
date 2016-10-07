package com.vsct.dt.strowgr.admin.gui.resource.api;


import com.google.common.eventbus.EventBus;
import com.vsct.dt.strowgr.admin.core.EntryPointKey;
import com.vsct.dt.strowgr.admin.core.repository.EntryPointRepository;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.core.event.out.DeleteEntryPointEvent;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

import static javax.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class EntrypointResourcesTest {

    @Test
    public void should_send_delete_entrypoint_event_and_return_204_when_delete_an_entrypoint() {
        // given
        EntryPointRepository entryPointRepository = mock(EntryPointRepository.class);
        EventBus eventBus = mock(EventBus.class);
        EntrypointResources entrypointResources = new EntrypointResources(eventBus, entryPointRepository);
        when(entryPointRepository.removeEntrypoint(any(EntryPointKey.class))).thenReturn(Optional.of(Boolean.TRUE));
        when(entryPointRepository.getCurrentConfiguration(any(EntryPointKey.class))).thenReturn(Optional.of(new EntryPoint("default-name", "hapadm",  "hapVersion", 0, new HashSet<>(), new HashSet<>(), new HashMap<>())));

        // test
        Response response = entrypointResources.deleteEntrypoint("MY_APP/MY_PLTF");

        // check
        assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
        verify(eventBus).post(any(DeleteEntryPointEvent.class));
        verify(entryPointRepository,times(1)).getEntryPointsId();
    }

    @Test
    public void should_not_post_delete_event_and_return_404_when_delete_an_non_existing_entrypoint() {
        // given
        EntryPointRepository entryPointRepository = mock(EntryPointRepository.class);
        EventBus eventBus = mock(EventBus.class);
        EntrypointResources entrypointResources = new EntrypointResources(eventBus, entryPointRepository);
        when(entryPointRepository.getCurrentConfiguration(any(EntryPointKey.class))).thenReturn(Optional.of(new EntryPoint("default-name", "hapadm", "hapVersion", 0, new HashSet<>(), new HashSet<>(), new HashMap<>())));
        when(entryPointRepository.getCurrentConfiguration(any(EntryPointKey.class))).thenReturn(Optional.empty());

        // test
        Response response = entrypointResources.deleteEntrypoint("MY_APP/MY_PLTF");

        // check
        assertThat(response.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
        verify(eventBus, times(0)).post(any(DeleteEntryPointEvent.class));
        verify(entryPointRepository,times(1)).getEntryPointsId();
    }

    @Test
    public void should_return_500_when_repository_cannot_remove_entrypoint_delete_entrypoint() {
        // given
        EntryPointRepository entryPointRepository = mock(EntryPointRepository.class);
        EntrypointResources entrypointResources = new EntrypointResources(null, entryPointRepository);
        when(entryPointRepository.removeEntrypoint(any(EntryPointKey.class))).thenReturn(Optional.empty());
        when(entryPointRepository.getCurrentConfiguration(any(EntryPointKey.class))).thenReturn(Optional.of(new EntryPoint("default-name", "hapadm", "hapVersion", 0, new HashSet<>(), new HashSet<>(), new HashMap<>())));

        // test
        Response response = entrypointResources.deleteEntrypoint("MY_APP/MY_PLTF");

        // check
        assertThat(response.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR.getStatusCode());
        verify(entryPointRepository,times(1)).getEntryPointsId();
    }
}