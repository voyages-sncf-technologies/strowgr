package com.vsct.dt.strowgr.admin.repository.consul;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsct.dt.strowgr.admin.core.EntryPointKeyDefaultImpl;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

public class ConsulRepositoryTest {
    @Test
    public void lock() throws Exception {
        ConsulReader mock = Mockito.mock(ConsulReader.class);
        CloseableHttpClient client = HttpClients.createDefault();
        ConsulRepository consulRepository = new ConsulRepository("host", 1234, 0, 100, new ObjectMapper(), mock,client);
        consulRepository.lock(new EntryPointKeyDefaultImpl("my id"));
    }

}