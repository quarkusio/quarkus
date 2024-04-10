package io.quarkus.it.cache.infinispan;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.Proto;
import org.infinispan.protostream.annotations.ProtoSchema;

import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;

@Path("/expensive-resource")
public class ExpensiveResource {

    private final AtomicInteger invocations = new AtomicInteger(0);

    @GET
    @Path("/{keyElement1}/{keyElement2}/{keyElement3}")
    @CacheResult(cacheName = "expensiveResourceCache")
    public ExpensiveResponse getExpensiveResponse(@PathParam("keyElement1") @CacheKey String keyElement1,
            @PathParam("keyElement2") @CacheKey String keyElement2, @PathParam("keyElement3") @CacheKey String keyElement3,
            @QueryParam("foo") String foo) {
        invocations.incrementAndGet();
        return new ExpensiveResponse(keyElement1 + " " + keyElement2 + " " + keyElement3 + " too!");
    }

    @POST
    @CacheInvalidateAll(cacheName = "expensiveResourceCache")
    public void invalidateAll() {

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
