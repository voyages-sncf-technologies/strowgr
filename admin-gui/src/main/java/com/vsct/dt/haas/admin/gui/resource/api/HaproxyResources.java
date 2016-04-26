package com.vsct.dt.haas.admin.gui.resource.api;

import com.vsct.dt.haas.admin.core.EntryPointRepository;
import com.vsct.dt.haas.admin.core.TemplateLocator;
import com.vsct.dt.haas.admin.template.locator.UriTemplateLocator;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@Path("/haproxy")
public class HaproxyResources {

    private final UriTemplateLocator   templateLocator;
    private final EntryPointRepository repository;

    public HaproxyResources(EntryPointRepository repository, UriTemplateLocator templateLocator) {
        this.repository = repository;
        this.templateLocator = templateLocator;
    }

    @GET
    @Path("/uri/{haproxyName : .+}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHaproxyURI(@PathParam("haproxyName") String haproxyName) throws IOException {
        return repository.getHaproxyVip(haproxyName).orElseThrow(() -> new RuntimeException("can't get haproxy uri of " + haproxyName));
    }

    @GET
    @Path("/template")
    @Produces(MediaType.TEXT_PLAIN)
    public String getCommittingHaproxyTemplate(@QueryParam("uri") String uri) throws IOException {
        if(uri == null || uri.equals("")){
            throw new BadRequestException("You must provide 'uri' query param");
        }
        return templateLocator.readTemplate(uri);
    }

}
