package io.quarkus.it.cache.infinispan;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.Proto;
import org.infinispan.protostream.annotations.ProtoSchema;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;

@Path("/expensive-resource")
public class ExpensiveResource {

    private final AtomicInteger invocations = new AtomicInteger(0);

    @Inject
    ClientRequestService requestService;

    @GET
    @Path("/{keyElement1}/{keyElement2}/{keyElement3}")
    @CacheResult(cacheName = "expensiveResourceCache")
    public ExpensiveResponse getExpensiveResponse(@PathParam("keyElement1") @CacheKey String keyElement1,
            @PathParam("keyElement2") @CacheKey String keyElement2,
            @PathParam("keyElement3") @CacheKey String keyElement3,
            @QueryParam("foo") String foo) {
        invocations.incrementAndGet();
        requestService.setData("getExpensiveResponse " + foo);
        return new ExpensiveResponse(keyElement1 + " " + keyElement2 + " " + keyElement3 + " too!");
    }

    @GET
    @Path("/async/{keyElement1}/{keyElement2}/{keyElement3}")
    @CacheResult(cacheName = "expensiveResourceCache")
    public Uni<ExpensiveResponse> getExpensiveResponseAsync(@PathParam("keyElement1") @CacheKey String keyElement1,
            @PathParam("keyElement2") @CacheKey String keyElement2,
            @PathParam("keyElement3") @CacheKey String keyElement3,
            @QueryParam("foo") String foo) {
        invocations.incrementAndGet();
        requestService.setData("getExpensiveResponseAsync " + foo);
        return Uni.createFrom()
                .item(new ExpensiveResponse(keyElement1 + " " + keyElement2 + " " + keyElement3 + " async too!"));
    }

    @DELETE
    @Path("/{keyElement1}/{keyElement2}/{keyElement3}")
    @CacheInvalidate(cacheName = "expensiveResourceCache")
    public Response resetExpensiveResponse(@PathParam("keyElement1") @CacheKey String keyElement1,
            @PathParam("keyElement2") @CacheKey String keyElement2, @PathParam("keyElement3") @CacheKey String keyElement3,
            @QueryParam("foo") String foo) {
        requestService.setData("invalidate");
        return Response.ok().build();
    }

    @DELETE
    @CacheInvalidateAll(cacheName = "expensiveResourceCache")
    public void invalidateAll() {
        requestService.setData("invalidateAll");
    }

    @GET
    @Path("/invocations")
    public int getInvocations() {
        return invocations.get();
    }

    @Proto
    public record ExpensiveResponse(String result) {
    }

    @ProtoSchema(includeClasses = { ExpensiveResponse.class })
    interface Schema extends GeneratedSchema {
    }
}
