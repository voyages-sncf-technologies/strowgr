package com.vsct.dt.strowgr.admin.repository.consul;

import org.apache.http.HttpResponse;
import org.junit.Test;
import org.mockito.Mockito;

public class ConsulRepositoryTest {

    @Test
    public void should_get_port_by_entrypoint() throws Exception {
        ConsulRepository consulRepository = new ConsulRepository("", 1234, 0, 64_000);
        consulRepository.getSessionFromHttpResponse(Mockito.mock(HttpResponse.class));
    }
}