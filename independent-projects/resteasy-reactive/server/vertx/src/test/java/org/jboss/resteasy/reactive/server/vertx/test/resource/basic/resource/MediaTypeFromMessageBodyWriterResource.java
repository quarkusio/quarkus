package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Variant;
import java.util.Arrays;
import java.util.List;

@Path("/")
public class MediaTypeFromMessageBodyWriterResource {

    @GET
    @Path("{type}")
    public Object hello(@PathParam("type") final String type, @HeaderParam("Accept") final String accept)
            throws Exception {
        return Class.forName(type).getDeclaredConstructor().newInstance();
    }

    @GET
    @Path("fixed")
    public Object fixedResponse(@QueryParam("type") @DefaultValue(MediaType.TEXT_PLAIN) final MediaType type) {
        final List<Integer> body = Arrays.asList(1, 2, 3, 4, 5, 6);
        return Response.ok(body, type).build();
    }

    @GET
    @Path("variants")
    public Response variantsResponse() {
        final List<Integer> body = Arrays.asList(1, 2, 3, 4, 5, 6);
        final List<Variant> variants = Variant
                .mediaTypes(MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE, MediaType.TEXT_PLAIN_TYPE).build();
        return Response.ok(body).variants(variants).build();
    }

    @GET
    @Path("variantsObject")
    public Object variantsObjectResponse() {
        final List<Integer> body = Arrays.asList(1, 2, 3, 4, 5, 6);
        final List<Variant> variants = Variant
                .mediaTypes(MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE, MediaType.TEXT_PLAIN_TYPE).build();
        return Response.ok(body).variants(variants).build();
    }

}
