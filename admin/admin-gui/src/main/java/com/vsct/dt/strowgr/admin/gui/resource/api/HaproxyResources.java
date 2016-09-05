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

import com.vsct.dt.strowgr.admin.core.EntryPointRepository;
import com.vsct.dt.strowgr.admin.core.TemplateGenerator;
import com.vsct.dt.strowgr.admin.gui.mapping.json.EntryPointWithPortsMappingJson;
import com.vsct.dt.strowgr.admin.template.IncompleteConfigurationException;
import com.vsct.dt.strowgr.admin.template.locator.UriTemplateLocator;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.Map;

import static javax.ws.rs.core.Response.ok;

@Path("/haproxy")
public class HaproxyResources {

    private final UriTemplateLocator templateLocator;
    private final EntryPointRepository repository;
    private final TemplateGenerator templateGenerator;

    public HaproxyResources(EntryPointRepository repository, UriTemplateLocator templateLocator, TemplateGenerator templateGenerator) {
        this.repository = repository;
        this.templateLocator = templateLocator;
        this.templateGenerator = templateGenerator;
    }

    @PUT
    @Path("/{haproxyId : .+}/{key: .+}/{value : .+}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response setHaproxyVip(@PathParam("haproxyId") String haproxyId, @PathParam("key") String key, @PathParam("value") String value) {
        repository.setHaproxyProperty(haproxyId, key, value);
        return ok().build();
    }

    @GET
    @Path("/{haproxyId : .+}/vip")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHaproxyVip(@PathParam("haproxyId") String haproxyId) {
        return repository.getHaproxyVip(haproxyId).orElseThrow(() -> new RuntimeException("can't get haproxy uri of " + haproxyId));
    }

    @GET
    @Path("{haproxyId : .+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String,Map<String,String>> getHaproxy(@PathParam("haproxyId") String haproxyId) {
        return repository.getHaproxyProperties(haproxyId).orElseThrow(() -> new RuntimeException("can't get haproxy uri of " + haproxyId));
    }

    @GET
    @Path("/template")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHaproxyTemplate(@QueryParam("uri") String uri) {
        if (uri == null || uri.equals("")) {
            throw new BadRequestException("You must provide 'uri' query param");
        }
        return templateLocator.readTemplate(uri).orElseThrow(() -> new NotFoundException("Could not find any template at " + uri));
    }

    @POST
    @Path("/template/valorise")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String getHaproxyConfiguration(@Valid EntryPointWithPortsMappingJson configuration) {
        try {
            String uri = configuration.getContext().get(UriTemplateLocator.URI_FIELD);
            String template = templateLocator.readTemplate(uri).orElseThrow(() -> new NotFoundException("Could not find any template at " + uri));
            return templateGenerator.generate(template, configuration, configuration.generatePortMapping());
        } catch (IncompleteConfigurationException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

}
