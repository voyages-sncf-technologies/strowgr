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
import com.vsct.dt.strowgr.admin.core.security.model.Platform;
import com.vsct.dt.strowgr.admin.core.security.model.User;
import com.vsct.dt.strowgr.admin.gui.mapping.json.EntryPointWithPortsMappingJson;
import com.vsct.dt.strowgr.admin.gui.mapping.json.HaproxyMappingJson;
import com.vsct.dt.strowgr.admin.repository.consul.ConsulRepository;

import io.dropwizard.auth.Auth;

import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static javax.ws.rs.core.Response.*;

@Path("/haproxy")
public class HaproxyResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(HaproxyResources.class);
	
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
    public Response createHaproxy(@Auth final User user, @PathParam("haproxyId") String haproxyId, @Valid HaproxyMappingJson haproxyMappingJson) {
    	LOGGER.info("createHaproxy for haproxyId={}", haproxyId);
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


    /**
     * Use only for {@link AggregateProxyResources}: useful to get empty haproxy, and not error 404 ( {@link ConsulRepository#getHaproxyProperties()} throw {@link RuntimeException} ) 
     * @param user
     * @param haproxyId
     * @return {@link Response}
     */
    public boolean getHaproxyAndAccepting404(@Auth final User user, @PathParam("haproxyId") String haproxyId) {

    	Optional<Map<String, String>> haProxy	=	 repository.getHaproxyPropertiesAndAccepting404(haproxyId);
    	
    	LOGGER.error("user={},haProxy={}", user, haProxy);
    	
    	if (!haProxy.isPresent() || !user.getPlatformValue().equalsIgnoreCase(haProxy.get().get("platform")) || user.isProdUser()) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll(@Auth final User user) {
    	List<Map<String, String>> haProxies	=	repository.getHaproxyProperties();
    	if ((haProxies !=null && haProxies.size() > 0) && (user !=null && !user.isProdUser())) {
    		haProxies.removeIf(haProxy -> user.getPlatformValue().equalsIgnoreCase(haProxy.get("platform")));
    	}
    	LOGGER.info("haProxies = {}", haProxies);
        return ok(haProxies).build();
    }
    
    /**
     * Return the list of plateform, previously hardcoded in view part :<br/>
     * @TODO: see later to store theses values outside application, maybe in consul?
     * @param user
     * @return {@link Set} platform list
     */
    @GET
    @Path("/platforms")    
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> getPlatforms(@Auth final User user) {
    	LOGGER.info("getPlatforms for user {}", user);
    	Set<String> platforms	=	new HashSet<String>()  {{
    	    add("assemblage");
    	    add("performance");
    	    add("integration");
    	    add("recette");
    	    add("preproduction");
    	    add("production");
    	}};
    	if (user!=null &&  !user.isProdUser()) {
        	LOGGER.info("remove platform {}", user.getPlatformValue());
    		platforms.remove(user.getPlatformValue());
    	}
    	return platforms;
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
