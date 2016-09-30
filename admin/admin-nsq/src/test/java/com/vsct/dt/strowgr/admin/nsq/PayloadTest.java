/*
 * Copyright (C) 2016 VSCT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.vsct.dt.strowgr.admin.nsq;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsct.dt.strowgr.admin.nsq.payload.*;
import com.vsct.dt.strowgr.admin.nsq.payload.fragment.Header;
import com.vsct.dt.strowgr.admin.nsq.payload.fragment.Server;
import com.vsct.dt.strowgr.admin.nsq.payload.fragment.Sidekick;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class PayloadTest {
    private ObjectMapper mapper;

    @Before
    public void setUp() throws Exception {
        mapper = new ObjectMapper();
    }

    private void assertSerialization(Object message, String file) throws IOException, URISyntaxException {
        String result = mapper.writer(
                new DefaultPrettyPrinter()
                        .withArrayIndenter(new DefaultPrettyPrinter.FixedSpaceIndenter())
        ).writeValueAsString(message);

        URL resource = getClass().getClassLoader().getResource(file);
        String expected = new String(Files.readAllBytes(Paths.get(resource.toURI())));
        assertEquals(expected.replaceAll("\r\n", "\n"), result.replaceAll("\r\n", "\n"));
    }

    @Test
    public void testCommitRequested() throws IOException, URISyntaxException {
        CommitRequested message = new CommitRequested("test-id", "TST", "REL1", "abcde", "fghij", null);
        message.getHeader().setTimestamp(1L);
        message.getHeader().setSource("test");
        assertSerialization(message, "commitRequested.expected.json");
    }

    @Test
    public void testCommitCompleted() throws IOException, URISyntaxException {
        CommitCompleted message = new CommitCompleted(new Header("test-id", "TST", "REL1"));
        message.getHeader().setTimestamp(1L);
        message.getHeader().setSource("test");
        assertSerialization(message, "commitCompleted.expected.json");
    }

    @Test
    public void testCommitFailed() throws IOException, URISyntaxException {
        CommitFailed message = new CommitFailed(new Header("test-id", "TST", "REL1"));
        message.getHeader().setTimestamp(1L);
        message.getHeader().setSource("test");
        assertSerialization(message, "commitFailed.expected.json");
    }

    @Test
    public void testDeleteRequested() throws IOException, URISyntaxException {
        DeleteRequested message = new DeleteRequested("test-id", "TST", "REL1", "test");
        message.getHeader().setTimestamp(1L);
        assertSerialization(message, "deleteRequested.expected.json");
    }

    @Test
    public void should_serialize_register_server_with_expected_format() throws IOException, URISyntaxException {
        // given
        Server server = new Server();
        server.setId("server-id");
        server.setPort("1234");
        server.setBackendId("backend-id");
        server.setIp("1.2.3.4");
        Map<String, String> context = new HashMap<>();
        context.put("key1", "val1");
        context.put("key2", "val2");
        server.setContext(context);
        RegisterServer message = new RegisterServer(new Header("test-id", "TST", "REL1"), server);
        message.getHeader().setTimestamp(1L);
        message.getHeader().setSource("test");

        // test and check
        assertSerialization(message, "registerServer.expected.json");
    }

    @Test
    public void registerSidekick() throws IOException, URISyntaxException {
        RegisterSidekick message = new RegisterSidekick("test-id", "TST", "REL1");
        message.getHeader().setTimestamp(1L);
        message.getHeader().setSource("test");

        Sidekick sidekick = new Sidekick();
        sidekick.setId("sidekcik-id");
        sidekick.setHost("host");
        sidekick.setRole("master");
        sidekick.setVip("2.3.4.5");
        sidekick.setVersion("1.0");
        message.setSidekick(sidekick);
        assertSerialization(message, "registerSidekick.expected.json");
    }

    @Test
    public void should_deserialized_commit_completed_message() throws IOException {
        // given
        String payload = "{\"header\":{\"correlationId\":\"16f484b9-3935-415b-bd8e-aaeaaf1020ac\",\"application\":\"STR\",\"platform\":\"REL1\",\"timestamp\":1467824168749,\"source\":\"sidekick-default-name-master\"}}";

        // test
        CommitCompleted commitCompleted = new ObjectMapper().readValue(payload.getBytes(), CommitCompleted.class);

        // check
        assertNotNull(commitCompleted);
        assertNotNull(commitCompleted.getHeader());
        assertEquals("16f484b9-3935-415b-bd8e-aaeaaf1020ac", commitCompleted.getHeader().getCorrelationId());
    }
}