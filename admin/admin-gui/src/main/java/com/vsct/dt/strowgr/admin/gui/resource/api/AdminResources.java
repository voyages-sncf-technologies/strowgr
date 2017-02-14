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
package com.vsct.dt.strowgr.admin.gui.resource.api;

import fr.vsct.dt.nsq.ServerAddress;
import fr.vsct.dt.nsq.lookup.NSQLookup;

import javax.ws.rs.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

/**
 * All resources about admin of Strowgr.
 */
@Path("/admin")
public class AdminResources {

    private final NSQLookup nsqLookup;

    public AdminResources(NSQLookup nsqLookup) {
        this.nsqLookup = nsqLookup;
    }

    @GET
    @Path("/version")
    public String version() throws IOException, URISyntaxException {
        return new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/version"))).readLine();
    }

    @GET
    @Path("/nsqlookup/{topic : .+}")
    public String lookupTopic(@PathParam("topic") String topic) throws IOException, URISyntaxException {
        return nsqLookup.lookup(topic).stream().map(ServerAddress::toString).reduce((s, s2) -> s + "<br>" + s2).orElse("can't find topic on nsq lookups");
    }

    @POST
    @Path("/version")
    public void lookupTopic(@QueryParam("host") String host, @QueryParam("port") int port) throws IOException, URISyntaxException {
        nsqLookup.addLookupAddress(host, port);
    }
}
