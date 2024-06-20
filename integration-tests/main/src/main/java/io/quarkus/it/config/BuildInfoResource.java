package io.quarkus.it.config;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/build-info")
public class BuildInfoResource {

    @ConfigProperty(name = "quarkus.build.timestamp")
    Instant buildTimestamp;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return DateTimeFormatter.ISO_INSTANT.format(buildTimestamp);
    }
}
