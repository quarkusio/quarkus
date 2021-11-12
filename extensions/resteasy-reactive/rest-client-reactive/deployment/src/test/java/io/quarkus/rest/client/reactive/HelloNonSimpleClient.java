package io.quarkus.rest.client.reactive;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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
}
