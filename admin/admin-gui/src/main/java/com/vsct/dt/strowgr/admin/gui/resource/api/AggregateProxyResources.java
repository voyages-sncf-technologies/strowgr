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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vsct.dt.strowgr.admin.core.security.model.User;
import com.vsct.dt.strowgr.admin.gui.mapping.json.EntryPointMappingJson;

import io.dropwizard.auth.Auth;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Proxy d'agregation pour {@link EntryPointResources} && {@link HaproxyResources}
 * @author VSC
 *
 */
@Path("/entrypoints")
public class AggregateProxyResources {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AggregateProxyResources.class);

    private final EntryPointResources entryPointResources;
    
    private final HaproxyResources haproxyResources;    

    public AggregateProxyResources(EntryPointResources entryPointResources, HaproxyResources haproxyResources) {
        this.entryPointResources	=	entryPointResources;
        this.haproxyResources	=	haproxyResources;
    }

    @GET
    @Path("/protected")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String>  getEntryPoints(@Auth final User user) throws JsonProcessingException {
    	
    	Set<String> entryPointsFinal	=	new HashSet<>();
    	Set<String> entryPoints	=	entryPointResources.getEntryPoints(user);
    	
    	LOGGER.trace("Found entryPoints {}", entryPoints);
    	
    	if (entryPoints!=null) {
    		for (String entryPoint: entryPoints) {
    			
    			// Récupération du détail de entryPoint, dont l'id du haproxy
    			EntryPointMappingJson entryPointMappingJson =	 entryPointResources.getCurrent(user, entryPoint);
    			String haproxyId	=	entryPointMappingJson.getHaproxy();
    			if (StringUtils.isEmpty(haproxyId)) {
    				entryPointsFinal.add(entryPoint);
    			} else {
	    			// Récupération du détail du ha proxy: null si non autorisé.
	    			boolean accept	=	haproxyResources.getHaproxyAndAccepting404(user, haproxyId);
	    			if (accept) {
	    				entryPointsFinal.add(entryPoint);
	    			}
    			}
    		}
    	}
        return entryPointsFinal;
    }


}
