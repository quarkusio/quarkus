package org.acme.quarkus.sample;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.acme.example.extension.runtime.ExampleBuildOptions;
import org.acme.example.extension.runtime.ExampleRuntimeConfig;

@Path("/hello")
public class HelloResource {

    @Inject
    ExampleBuildOptions buildOptions;

    @Inject
    ExampleRuntimeConfig rtConfig;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello " + buildOptions.name;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/runtime-name")
    public String runtimeName() {
        return rtConfig.runtimeName;
    }
}
