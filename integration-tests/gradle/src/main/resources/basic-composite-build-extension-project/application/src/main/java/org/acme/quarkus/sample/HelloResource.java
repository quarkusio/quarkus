package org.acme.quarkus.sample;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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