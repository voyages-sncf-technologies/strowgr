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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * All resources about admin of Strowgr.
 */
@Path("/admin")
public class AdminResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminResources.class);

    @GET
    @Path("/version")
    public String getVersion() throws IOException, URISyntaxException {
        URL versionURL = getClass().getClassLoader().getResource("version");
        if (versionURL == null) {
            LOGGER.error("can't find version file uri relative to classpath. Check 'version' is in root classpath.");
            throw new IllegalStateException("can't find version file uri relative to classpath. Check 'version' is in root classpath.");
        } else {
            return new String(Files.readAllBytes(Paths.get(versionURL.toURI())));
        }
    }
}
