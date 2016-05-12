package com.vsct.dt.haas.admin.gui.resource.api;

import com.vsct.dt.haas.admin.core.EntryPointRepository;
import com.vsct.dt.haas.admin.core.TemplateGenerator;
import com.vsct.dt.haas.admin.gui.mapping.json.EntryPointWithPortsMappingJson;
import com.vsct.dt.haas.admin.template.IncompleteConfigurationException;
import com.vsct.dt.haas.admin.template.locator.UriTemplateLocator;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

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
    public String getHaproxyURI(@PathParam("haproxyName") String haproxyName) {
        return repository.getHaproxyVip(haproxyName).orElseThrow(() -> new RuntimeException("can't get haproxy uri of " + haproxyName));
    }

    @GET
    @Path("/template")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHaproxyTemplate(@QueryParam("uri") String uri) {
        if (uri == null || uri.equals("")) {
            throw new BadRequestException("You must provide 'uri' query param");
        }
        return templateLocator.readTemplate(uri).orElseThrow(() -> new NotFoundException("Could not find any template at " + uri));
    }

    @POST
    @Path("/template/valorise")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String getHaproxyConfiguration(@Valid EntryPointWithPortsMappingJson configuration) {
        try {
            String uri = configuration.getContext().get(UriTemplateLocator.URI_FIELD);
            String template = templateLocator.readTemplate(uri).orElseThrow(() -> new NotFoundException("Could not find any template at " + uri));
            return templateGenerator.generate(template, configuration, configuration.generatePortMapping());
        } catch (IncompleteConfigurationException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

}
