package io.quarkus.rest.client.reactive;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;

// Test clients with non-simple type (e.g. primitive arrays)
// are generated as valid classes.
//
// https://github.com/quarkusio/quarkus/issues/21375
//
@RegisterRestClient(configKey = "hello2")
public interface HelloNonSimpleClient {
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Path("/bytes")
    byte[] echoSyncBytes(byte[] value);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/ints")
    Integer[] echoSyncInts(Integer[] value);

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Path("/bytes")
    Uni<byte[]> echoAsyncBytes(byte[] value);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/ints")
    Uni<Integer[]> echoAsyncInts(Integer[] value);

    @GET
    @Path("/query-params-to-map")
    Map<String, String> echoQueryAsMap(@QueryParam("p1") String p1, @QueryParam("p2(") String p2,
            @QueryParam("p3[") String p3, @QueryParam("p4?") String p4, @QueryParam("p5=") String p5,
            @QueryParam("p6-") String p6);
}
