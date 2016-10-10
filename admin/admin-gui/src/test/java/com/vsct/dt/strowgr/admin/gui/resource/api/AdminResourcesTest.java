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

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class AdminResourcesTest {


    @Test
    public void should_return_current_version() throws IOException, URISyntaxException {
        // given
        String versionExpected = new String(Files.readAllBytes(Paths.get(ClassLoader.getSystemResource("version-expected").toURI())));

        // test
        String version = new AdminResources().getVersion();

        // check
        Assert.assertEquals(versionExpected, version);
    }
}