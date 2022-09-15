package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("absolutePath")
public class GetAbsolutePathResource {

    @GET
    public Response response(@QueryParam("dummy") String dummy, @Context UriInfo uriInfo) {
        return Response.ok()
                .header("absolutePath", uriInfo.getAbsolutePath().toString())
                .header("dummy", dummy == null ? "unset" : dummy)
                .build();
    }
}
