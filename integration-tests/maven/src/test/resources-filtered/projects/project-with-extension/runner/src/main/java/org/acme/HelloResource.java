package org.acme;

import org.eclipse.microprofile.config.inject.ConfigProperty;


import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class HelloResource {

    final CommonBean common;
    final LibraryBean library;

    @Inject
    @ConfigProperty(name = "greeting")
    String greeting;

    @Inject
    ModuleList moduleList;

    public HelloResource(CommonBean common, LibraryBean library) {
        this.common = java.util.Objects.requireNonNull(common);
        this.library = java.util.Objects.requireNonNull(library);
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello";
    }

    @GET
    @Path("/greeting")
    @Produces(MediaType.TEXT_PLAIN)
    public String greeting() {
        return greeting;
    }

    @GET
    @Path("/local-modules")
    @Produces(MediaType.TEXT_PLAIN)
    public String localModules() {
        return moduleList.getModules().toString();
    }
}
