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

import com.vsct.dt.strowgr.admin.core.IncompleteConfigurationException;
import com.vsct.dt.strowgr.admin.core.TemplateGenerator;
import com.vsct.dt.strowgr.admin.core.TemplateLocator;
import com.vsct.dt.strowgr.admin.core.repository.HaproxyRepository;
import com.vsct.dt.strowgr.admin.core.security.model.User;
import com.vsct.dt.strowgr.admin.gui.mapping.json.EntryPointWithPortsMappingJson;
import com.vsct.dt.strowgr.admin.gui.mapping.json.HaproxyMappingJson;

import io.dropwizard.auth.Auth;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Set;

import static javax.ws.rs.core.Response.*;

@Path("/haproxy")
public class HaproxyResources {

    private final TemplateLocator   templateLocator;
    private final HaproxyRepository repository;
    private final TemplateGenerator templateGenerator;

    public HaproxyResources(HaproxyRepository repository, TemplateLocator templateLocator, TemplateGenerator templateGenerator) {
        this.repository = repository;
        this.templateLocator = templateLocator;
        this.templateGenerator = templateGenerator;
    }

    @PUT
    @Path("/{haproxyId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createHaproxy(@Auth final User user, @PathParam("haproxyId") String haproxyId, @NotNull @Valid HaproxyMappingJson haproxyMappingJson) {
        repository.setHaproxyProperty(haproxyId, "name", haproxyMappingJson.getName());
        haproxyMappingJson.getBindings().forEach((key, value) -> repository.setHaproxyProperty(haproxyId, "binding/"+key, value));
        repository.setHaproxyProperty(haproxyId, "platform", haproxyMappingJson.getPlatform());
        repository.setHaproxyProperty(haproxyId, "autoreload", String.valueOf(haproxyMappingJson.getAutoreload()));
        return created(URI.create("/haproxy/" + haproxyId)).build();
    }

    @PUT
    @Path("/{haproxyId}/binding/{bindingId}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response setHaproxyBindings(@Auth final User user,  @PathParam("haproxyId") String haproxyId, @PathParam("bindingId") String bindingId, @NotEmpty String value) {
        repository.setHaproxyProperty(haproxyId, "binding/" + bindingId, value);
        return created(URI.create("/haproxy/" + haproxyId + "/binding/" + bindingId)).build();
    }

    @GET
    @Path("/{haproxyId}/binding/{bindingId}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getHaproxyBinding(@Auth final User user,  @PathParam("haproxyId") String haproxyId, @PathParam("bindingId") String bindingId) {
        return repository.getHaproxyProperty(haproxyId, "binding/" + bindingId)
                .map(vip -> ok(vip).build())
                .orElseGet(() -> status(Response.Status.NOT_FOUND).entity("can't get haproxy uri of " + haproxyId).build());
    }

    @GET
    @Path("{haproxyId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHaproxy(@Auth final User user, @PathParam("haproxyId") String haproxyId) {
        return repository.getHaproxyProperties(haproxyId)
                .map(props -> ok(props).build())
                .orElseGet(() -> status(Response.Status.NOT_FOUND).entity("can't get haproxy properties of " + haproxyId).build());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll(@Auth final User user) {
        return ok(repository.getHaproxyProperties()).build();
    }

    @GET
    @Path("/ids")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> getHaproxyIds(@Auth final User user) {
        return repository.getHaproxyIds();
    }

    @POST
    @Path("/template/valorise")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String getHaproxyConfiguration(@Auth final User user, @NotNull @Valid EntryPointWithPortsMappingJson configuration) {
        try {
            String template = templateLocator.readTemplate(configuration).orElseThrow(() -> new NotFoundException("Could not find any template for entrypoint " + configuration));
            return templateGenerator.generate(template, configuration, configuration.generatePortMapping());
        } catch (IncompleteConfigurationException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @GET
    @Path("/versions")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> getHaproxyVersions(@Auth final User user){
        return repository.getHaproxyVersions();
    }

    @PUT
    @Path("/versions/{haproxyVersion}")
    public void addHaproxyVersion(@Auth final User user, @PathParam("haproxyVersion") String haproxyVersion){
        repository.addVersion(haproxyVersion);
    }

}
