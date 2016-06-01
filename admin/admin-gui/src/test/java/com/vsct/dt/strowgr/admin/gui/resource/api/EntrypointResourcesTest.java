package com.vsct.dt.strowgr.admin.gui.resource.api;


import com.vsct.dt.strowgr.admin.core.EntryPointKeyDefaultImpl;
import com.vsct.dt.strowgr.admin.core.EntryPointRepository;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EntrypointResourcesTest {

    @Test
    public void should_return_204_when_delete_an_entrypoint() {
        // given
        String id = "MY_APP/MY_PLTF";
        EntryPointRepository entryPointRepository = mock(EntryPointRepository.class);
        EntrypointResources entrypointResources = new EntrypointResources(null, entryPointRepository);
        when(entryPointRepository.removeEntrypoint(new EntryPointKeyDefaultImpl(id))).thenReturn(Boolean.TRUE);

        // test
        Response response = entrypointResources.deleteEntrypoint("MY_APP/MY_PLTF");

        // check
        assertThat(response.getStatus()).isEqualTo(NO_CONTENT.getStatusCode());
    }

    @Test
    public void should_return_404_when_delete_an_non_existing_entrypoint() {
        // given
        String id = "MY_APP/MY_PLTF";
        EntryPointRepository entryPointRepository = mock(EntryPointRepository.class);
        EntrypointResources entrypointResources = new EntrypointResources(null, entryPointRepository);
        when(entryPointRepository.removeEntrypoint(new EntryPointKeyDefaultImpl(id))).thenReturn(Boolean.FALSE);

        // test
        Response response = entrypointResources.deleteEntrypoint("MY_APP/MY_PLTF");

        // check
        assertThat(response.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
    }

    @Test
    public void should_return__when_delete_an_non_existing_entrypoint() {
        // given
        String id = "MY_APP/MY_PLTF";
        EntryPointRepository entryPointRepository = mock(EntryPointRepository.class);
        EntrypointResources entrypointResources = new EntrypointResources(null, entryPointRepository);
        when(entryPointRepository.removeEntrypoint(new EntryPointKeyDefaultImpl(id))).thenReturn(null);

        // test
        Response response = entrypointResources.deleteEntrypoint("MY_APP/MY_PLTF");

        // check
        assertThat(response.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR.getStatusCode());
    }
}