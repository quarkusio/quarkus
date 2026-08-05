package org.acme.quarkus.sample;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.acme.example.extension.runtime.QuarkusExampleExtensionConfig;
import org.acme.liba.LibA;
import org.acme.libb.LibB;

@Path("/hello")
public class HelloResource {

    private static final String BUILD_MESSAGE_PROPERTY = "org.acme.example.extension.build-message";

    @Inject
    LibB libB;
    @Inject
    LibA libA;

    @Inject
    private QuarkusExampleExtensionConfig config;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello from " + libB.getName() + " and " + libA.getName() + " extension enabled: " + config.enabled()
                + " build message: " + System.getProperty(BUILD_MESSAGE_PROPERTY);
    }
}
