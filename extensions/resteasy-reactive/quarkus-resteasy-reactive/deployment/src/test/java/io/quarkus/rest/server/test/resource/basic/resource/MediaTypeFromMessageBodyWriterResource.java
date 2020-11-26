package io.quarkus.rest.server.test.resource.basic.resource;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

@Path("/")
public class MediaTypeFromMessageBodyWriterResource {

    @GET
    @Path("{type}")
    public Object hello(@PathParam("type") final String type, @HeaderParam("Accept") final String accept)
            throws Exception {
        return Class.forName(type).newInstance();
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
