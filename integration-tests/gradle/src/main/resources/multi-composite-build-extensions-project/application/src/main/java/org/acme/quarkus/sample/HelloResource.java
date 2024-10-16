package org.acme.quarkus.sample;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.acme.libb.LibB;
import org.acme.liba.LibA;
import org.acme.example.extension.runtime.QuarkusExampleExtensionConfig;

@Path("/hello")
public class HelloResource {

    @Inject
    LibB libB;
    @Inject
    LibA libA;

    @Inject
    private QuarkusExampleExtensionConfig config;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello from " + libB.getName()+" and "+libA.getName()+" extension enabled: "+config.enabled;
    }
}
