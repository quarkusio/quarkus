package org.test;

import java.util.*;
import java.util.stream.*;

import javax.inject.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("/jpa-build-info")
public class HelloResource {

    @Inject
    JacksonCustomizer customizer;

    @GET
    @Path("customizeCalled")
    @Produces(MediaType.TEXT_PLAIN)
    public int customizeCalled() {
        return customizer.customizeCalled();
    }

    @GET
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> all() {
        return customizer.getAll().stream()
                .map(Class::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    @GET
    @Path("entities")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> entities() {
        return customizer.getEntities().stream()
                .map(Class::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    @GET
    @Path("others")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> others() {
        return customizer.getOthers().stream()
                .map(Class::getName)
                .sorted()
                .collect(Collectors.toList());
    }
}