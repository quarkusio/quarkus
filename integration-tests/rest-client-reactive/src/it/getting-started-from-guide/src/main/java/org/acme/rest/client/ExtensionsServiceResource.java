package org.acme.rest.client;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Path("/extensions")
public class ExtensionsServiceResource {

    @GET
    public Set<Extension> getById(@QueryParam("id") String id) {
        if ("io.quarkus:quarkus-rest-client-reactive".equals(id)) {
            Extension extension = new Extension();
            extension.id = id;
            extension.name = "REST Client Reactive";
            extension.keywords = List.of("rest-client", "reactive");
            return Set.of(extension);
        }

        return Collections.emptySet();
    }

    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public String registerFile(File file) {
        return "done";
    }
}