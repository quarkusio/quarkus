package io.quarkus.it.rest.client.main;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("")
public interface PrimitiveClient {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    long getLong(@QueryParam("value") String value);

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    int getInt(@QueryParam("value") String value);

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    double getDouble(@QueryParam("value") String value);

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    boolean getBoolean(@QueryParam("value") String value);

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    Long getLongWrapper(@QueryParam("value") String value);
}
