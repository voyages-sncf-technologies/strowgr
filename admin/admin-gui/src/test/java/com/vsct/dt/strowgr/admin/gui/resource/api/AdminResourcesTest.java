/*
 *  Copyright (C) 2016 VSCT
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.vsct.dt.strowgr.admin.gui.resource.api;

import fr.vsct.dt.nsq.ServerAddress;
import fr.vsct.dt.nsq.lookup.NSQLookup;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AdminResourcesTest {


    @Test
    public void should_return_current_version() throws IOException, URISyntaxException {
        // given
        String versionExpected = new String(Files.readAllBytes(Paths.get(ClassLoader.getSystemResource("version-expected").toURI())));

        // test
        String version = new AdminResources(null).version();

        // check
        Assert.assertEquals(versionExpected, version);
    }

    @Test
    public void should_return_list_of_nsqlookup() throws IOException, URISyntaxException {
        // given
        NSQLookup nsqLookup = mock(NSQLookup.class);
        HashSet<ServerAddress> serverAddresses = new HashSet<>();
        serverAddresses.add(new ServerAddress("localhost", 1234));
        serverAddresses.add(new ServerAddress("1.2.3.4", 1111));
        when(nsqLookup.lookup("mytopic")).thenReturn(serverAddresses);

        // test
        String resultAddresses = new AdminResources(nsqLookup).lookupTopic("mytopic");

        // check
        Assert.assertEquals("localhost:1234<br>1.2.3.4:1111", resultAddresses);
    }

}