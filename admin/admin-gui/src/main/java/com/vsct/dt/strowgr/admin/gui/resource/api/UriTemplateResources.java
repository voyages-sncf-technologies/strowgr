/*
 * Copyright (C) 2016 VSCT
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

import com.vsct.dt.strowgr.admin.core.TemplateGenerator;
import com.vsct.dt.strowgr.admin.core.TemplateLocator;
import com.vsct.dt.strowgr.admin.template.locator.UriTemplateLocator;
import org.hibernate.validator.constraints.NotEmpty;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;

@Path("/templates")
public class UriTemplateResources {

    private final UriTemplateLocator templateLocator;
    private final TemplateGenerator  templateGenerator;

    public UriTemplateResources(UriTemplateLocator templateLocator, TemplateGenerator templateGenerator){
        this.templateLocator = templateLocator;
        this.templateGenerator = templateGenerator;
    }

    //TODO should be in its own resource
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getHaproxyTemplate(@QueryParam("uri") @NotEmpty String uri) {
        if (uri == null || uri.equals("")) {
            throw new BadRequestException("You must provide 'uri' query param");
        }
        return templateLocator.readTemplate(uri)
                .map(template -> ok(template).build())
                .orElseGet(() -> status(Response.Status.NOT_FOUND).entity("Could not find any template at " + uri).build());
    }

    //TODO should be in its own resource
    @GET
    @Path("/frontbackends")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFrontAndBackends(@QueryParam("uri") @NotEmpty String uri) {
        if (uri == null || uri.equals("")) {
            throw new BadRequestException("You must provide 'uri' query param");
        }
        return templateLocator.readTemplate(uri)
                .map(templateGenerator::generateFrontAndBackends)
                .map(map -> ok(map).build())
                .orElseGet(() -> status(Response.Status.NOT_FOUND).entity("Could not find any template at " + uri).build());
    }


}
