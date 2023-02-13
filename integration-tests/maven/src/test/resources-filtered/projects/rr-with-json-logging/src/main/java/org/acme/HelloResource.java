package org.acme;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class HelloResource {

    @ConfigProperty(name = "greeting")
    String greeting;

    @ConfigProperty(name = "quarkus.application.version")
    String applicationVersion;

    @ConfigProperty(name = "quarkus.application.name")
    String applicationName;

    @ConfigProperty(name = "other.greeting", defaultValue = "other")
    String otherGreeting;

    @ConfigProperty(name = "quarkus.profile")
    String profile;

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
    @Path("/package")
    @Produces(MediaType.TEXT_PLAIN)
    public String pkg() {
        return Blah.class.getPackage().getName();
    }

    @GET
    @Path("/nameAndVersion")
    @Produces(MediaType.TEXT_PLAIN)
    public String nameAndVersion() {
        return applicationName + "/" +  applicationVersion;
    }

    @GET
    @Path("/otherGreeting")
    @Produces(MediaType.TEXT_PLAIN)
    public String otherGreeting() {
        return otherGreeting;
    }

    @GET
    @Path("/profile")
    @Produces(MediaType.TEXT_PLAIN)
    public String profile() {
        return profile;
    }


    public static class Blah {
        
    }
}
