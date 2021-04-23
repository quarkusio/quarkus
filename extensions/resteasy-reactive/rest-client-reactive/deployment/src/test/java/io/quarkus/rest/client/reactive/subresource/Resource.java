package io.quarkus.rest.client.reactive.subresource;

import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

@Path("/path")
public class Resource {
    @GET
    @Path("{part1}/{part2}/{part3}")
    public String getUriParts(@RestPath String part1, @RestPath String part2, @RestPath String part3) {
        return String.format("%s/%s/%s", part1, part2, part3);
    }

    @POST
    @Path("{part1}/{part2}")
    public Response getUriEntityAndQueryParam(@RestPath String part1, @RestPath String part2, @RestQuery String queryParam,
            String entity, @Context HttpHeaders headers) {
        Response.ResponseBuilder responseBuilder = Response.ok(String.format("%s/%s:%s:%s", part1, part2, entity, queryParam));

        for (Map.Entry<String, List<String>> headerEntry : headers.getRequestHeaders().entrySet()) {
            String headerName = headerEntry.getKey();
            List<String> value = headerEntry.getValue();
            if (value.size() == 1 && !"Content-Length".equalsIgnoreCase(headerName)) {
                responseBuilder.header(headerName, value.get(0));
            }
        }
        return responseBuilder.build();
    }
}
