package com.vsct.dt.haas.admin.gui.resource.api;

import com.vsct.dt.haas.admin.core.EntryPointKey;
import com.vsct.dt.haas.admin.core.EntryPointKeyDefaultImpl;
import com.vsct.dt.haas.admin.core.EntryPointRepository;
import com.vsct.dt.haas.admin.core.TemplateGenerator;
import com.vsct.dt.haas.admin.core.configuration.EntryPoint;
import com.vsct.dt.haas.admin.gui.mapping.json.EntryPointMappingJson;
import com.vsct.dt.haas.admin.gui.mapping.json.EntryPointWithPortsMappingJson;
import com.vsct.dt.haas.admin.template.locator.UriTemplateLocator;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.HashMap;

@Path("/haproxy")
public class HaproxyResources {

    private final UriTemplateLocator   templateLocator;
    private final EntryPointRepository repository;
    private final TemplateGenerator    templateGenerator;

    public HaproxyResources(EntryPointRepository repository, UriTemplateLocator templateLocator, TemplateGenerator templateGenerator) {
        this.repository = repository;
        this.templateLocator = templateLocator;
        this.templateGenerator = templateGenerator;
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
    public String getHaproxyTemplate(@QueryParam("uri") String uri) throws IOException {
        if (uri == null || uri.equals("")) {
            throw new BadRequestException("You must provide 'uri' query param");
        }
        return templateLocator.readTemplate(uri);
    }

    @POST
    @Path("/template/valorise")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String getHaproxyConfiguration(@Valid EntryPointWithPortsMappingJson configuration) throws IOException {
        return generateHaproxyConfiguration(configuration);
    }

    private String generateHaproxyConfiguration(EntryPointWithPortsMappingJson configuration) {
        String template = templateLocator.readTemplate(configuration.getContext().get(UriTemplateLocator.URI_FIELD));
        return templateGenerator.generate(template, configuration, configuration.generatePortMapping());
    }

}
