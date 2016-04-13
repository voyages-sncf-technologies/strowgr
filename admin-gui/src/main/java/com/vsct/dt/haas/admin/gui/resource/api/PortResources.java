package com.vsct.dt.haas.admin.gui.resource.api;

import com.vsct.dt.haas.admin.core.PortProvider;

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
