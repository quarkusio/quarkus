package org.acme;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

@Path("/hello")
public class HelloResource {

    @Inject
    @ConfigProperty(name = "greeting")
    String greeting;

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
    @Path("/resourcesCount")
    @Produces(MediaType.TEXT_PLAIN)
    public int resourcesCount() throws IOException {
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources("META-INF/resources/index.html");
        int count = 0;
        while (resources.hasMoreElements()) {
            count++;
            resources.nextElement();
        }
        return count;
    }


    public static class Blah {
        
    }
}
