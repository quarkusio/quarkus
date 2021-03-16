package io.quarkus.test.reload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;

@Path("/")
@ApplicationScoped // see contents map
public class AdditionalWatchedResourcesResource {

    // make sure to load content only once to check whether app was reloaded or not
    // (TCCL.getResourceAsStream(name) will always provide the latest data, regardless of being reloaded or not)
    private final Map<String, String> contents = new HashMap<>();

    @Path("content/{name}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getContentOfAdditionallyWatchedResource(@PathParam("name") String name) {
        return contents.computeIfAbsent(name, k -> {
            try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(name)) {
                return in != null ? IOUtils.toString(in, StandardCharsets.UTF_8) : "";
            } catch (IOException e) {
                throw new IllegalStateException("Error reading " + name, e);
            }
        });
    }
}
