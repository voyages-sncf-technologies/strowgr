package com.vsct.dt.haas.dropwizard.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import com.vsct.dt.haas.events.AddNewEntryPointEvent;
import com.vsct.dt.haas.state.AdminState;
import com.vsct.dt.haas.state.EntryPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RestApiResources {

    private final EventBus eventBus;
    private final AdminState adminState;
    Logger LOGGER = LoggerFactory.getLogger(RestApiResources.class);

    ObjectMapper objectMapper = new ObjectMapper();

    public RestApiResources(AdminState adminState, EventBus eventBus) {
        this.eventBus = eventBus;
        this.adminState = adminState;
    }


    @POST
    @Path("/entrypoint")
    @Timed
    public String addEntryPoint(EntryPoint entryPoint) {
        LOGGER.info("Get all criteria");

        AddNewEntryPointEvent event = new AddNewEntryPointEvent(entryPoint);
        eventBus.post(event);

        return "Request posted, look info to follow actions";
    }

    @GET
    @Path("/infos")
    public String getInfos() throws JsonProcessingException {

        return objectMapper.writeValueAsString(adminState);

    }
}
