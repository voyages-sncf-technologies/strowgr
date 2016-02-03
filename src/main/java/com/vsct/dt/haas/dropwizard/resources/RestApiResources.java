package com.vsct.dt.haas.dropwizard.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import com.vsct.dt.haas.dropwizard.StringResponse;
import com.vsct.dt.haas.events.*;
import com.vsct.dt.haas.state.AdminState;
import com.vsct.dt.haas.state.EntryPoint;
import com.vsct.dt.haas.state.Server;
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
    public StringResponse addEntryPoint(EntryPoint entryPoint) {
        LOGGER.info("Get all criteria");

        AddNewEntryPointEvent event = new AddNewEntryPointEvent(entryPoint);
        eventBus.post(event);

        return new StringResponse("Request posted, look info to follow actions");
    }

    @GET
    @Path("/infos")
    public String getInfos() throws JsonProcessingException {

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adminState);
    }


    /* DEBUGGING METHODS */
    @GET
    @Path("/reload")
    public StringResponse reload(@QueryParam("application") String application, @QueryParam("platform") String platform) {
        UpdateEntryPointEvent event = new UpdateEntryPointEvent(application, platform);
        eventBus.post(event);
        return new StringResponse("Request posted, look info to follow actions");
    }

    @GET
    @Path("/ep-deployed")
    public StringResponse epDeployed(@QueryParam("application") String application, @QueryParam("platform") String platform){
        EntryPointDeployedEvent event = new EntryPointDeployedEvent(application, platform);
        eventBus.post(event);
        return new StringResponse("Request posted, look info to follow actions");
    }

    @GET
    @Path("/add-new-server")
    public StringResponse addNewServer(@QueryParam("application") String application,
                               @QueryParam("platform") String platform,
                               @QueryParam("backend") String backend,
                               @QueryParam("instanceName") String instanceName,
                               @QueryParam("name") String name,
                               @QueryParam("ip") String ip,
                               @QueryParam("port") String port){
        AddNewServerEvent event = new AddNewServerEvent(application, platform, backend, new Server(instanceName, name, ip, port));
        eventBus.post(event);
        return new StringResponse("Request posted, look info to follow actions");
    }

    @GET
    @Path("/ep-updated")
    public StringResponse epUpdated(@QueryParam("application") String application, @QueryParam("platform") String platform){
        CommitedEntryPointEvent event = new CommitedEntryPointEvent(application, platform);
        eventBus.post(event);
        return new StringResponse("Request posted, look info to follow actions");
    }


}
