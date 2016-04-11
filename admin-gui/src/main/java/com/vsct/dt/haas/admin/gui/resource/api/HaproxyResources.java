package com.vsct.dt.haas.admin.gui.resource.api;

import com.vsct.dt.haas.admin.core.EntryPointRepository;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@Path("/haproxy")
public class HaproxyResources {

    @GET
    @Path("/uri/{haproxyName : .+}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHaproxyURI(@PathParam("haproxyName") String haproxyName) throws IOException {
        return repository.getHaproxyVip(haproxyName).orElseThrow(() -> new RuntimeException("can't get haproxy uri of " + haproxyName));
    }

    private final EntryPointRepository repository;

    public HaproxyResources(EntryPointRepository repository) {
        this.repository = repository;
    }


}
