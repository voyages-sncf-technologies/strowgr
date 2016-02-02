package com.vsct.dt.haas.dropwizard.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.common.eventbus.EventBus;
import com.vsct.dt.haas.state.AdminState;
import com.vsct.dt.haas.state.EntryPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RestApiResources {
    private final AdminState adminState;
    Logger LOGGER = LoggerFactory.getLogger(RestApiResources.class);

    private EventBus eventBus;

    public RestApiResources() {
        eventBus = new EventBus();
        adminState = new AdminState();
        eventBus.register(adminState);
    }


    @POST
    @Path("/entrypoint/")
    @Timed
    public String getAllCriteria(EntryPoint entryPoint) {
        LOGGER.info("Get all criteria");

        return "hello";
    }
}
