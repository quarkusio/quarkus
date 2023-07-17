package org.acme;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import org.jboss.resteasy.reactive.RestPath;

@Path("/cp")
public class ClassPathResource {

    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello";
    }

    @GET
    @Path("/resourceCount/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public int resourceCount(@RestPath String name) throws IOException {
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources("META-INF/resources/" + name);
        int count = 0;
        while (resources.hasMoreElements()) {
            count++;
            resources.nextElement();
        }
        return count;
    }
}
