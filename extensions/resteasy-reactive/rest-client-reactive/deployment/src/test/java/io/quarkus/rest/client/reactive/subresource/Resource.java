package io.quarkus.rest.client.reactive.subresource;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

@Path("/path")
public class Resource {

    @GET
    @Path("{part1}/{part2}/{part3}")
    public String getUriParts(@RestPath String part1, @RestPath String part2, @RestPath String part3) {
        return String.format("%s/%s/%s", part1, part2, part3);
    }

    @GET
    @Path("{part1}/{part2}/{part3}/{part4}")
    public String getUriParts(@RestPath String part1, @RestPath String part2, @RestPath String part3, @RestPath String part4) {
        return String.format("%s/%s/%s/%s", part1, part2, part3, part4);
    }

    @GET
    @Path("{part1}/{part2}/{part3}/{part4}/{part5}")
    public String getUriParts(@RestPath String part1, @RestPath String part2, @RestPath String part3, @RestPath String part4,
            @RestPath String part5) {
        return String.format("%s/%s/%s/%s/%s", part1, part2, part3, part4, part5);
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

    @POST
    @Path("{part1}/{part2}/{part3}")
    public Response getUriEntityAndQueryParamFromSubResource(@RestPath String part1, @RestPath String part2,
            @RestPath String part3, @RestQuery String queryParam, String entity, @Context HttpHeaders headers) {
        Response.ResponseBuilder responseBuilder = Response
                .ok(String.format("%s/%s/%s:%s:%s", part1, part2, part3, entity, queryParam));

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
