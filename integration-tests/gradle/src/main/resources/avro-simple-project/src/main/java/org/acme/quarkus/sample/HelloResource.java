package org.acme.quarkus.sample;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.acme.quarkus.hello.Provider;

@Path("/hello")
public class HelloResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String providerValues() {
        return Arrays.stream(Provider.values()).map(String::valueOf).collect(joining(","));
    }
}