package org.acme.rest.client;

import io.smallrye.common.annotation.Blocking;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.POST;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

@Path("/extension")
public class ExtensionsResource {

    @RestClient
    ExtensionsService extensionsService;

    @GET
    @Path("/id/{id}")
    @Blocking
    public Set<Extension> id(String id) {
        return extensionsService.getById(id);
    }

    @POST
    @Path("/init")
    public void initialize() throws IOException {
        extensionsService.registerFile(Files.createTempFile("any", ".txt").toFile());
    }
}