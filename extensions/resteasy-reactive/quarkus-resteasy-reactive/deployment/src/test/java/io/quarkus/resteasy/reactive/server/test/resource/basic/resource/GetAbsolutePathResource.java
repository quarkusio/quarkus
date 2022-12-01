package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

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
