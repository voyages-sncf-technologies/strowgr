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

import com.vsct.dt.strowgr.admin.core.PortProvider;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

@Path("/ports")
public class PortResources {

    private final PortProvider portProvider;

    public PortResources(PortProvider portProvider) {
        this.portProvider = portProvider;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Integer> getPorts() {
        return portProvider.getPorts().orElseGet(HashMap::new);
    }

    @PUT
    @Path("/{id : .+}")
    public String setPort(@PathParam("id") String id) {
        return String.valueOf(portProvider.newPort(id));
    }

    @GET
    @Path("/{id : .+}")
    public String getPort(@PathParam("id") String id) {
        return portProvider.getPort(id)
                .map(String::valueOf)
                .orElseThrow(NotFoundException::new);
    }

}
