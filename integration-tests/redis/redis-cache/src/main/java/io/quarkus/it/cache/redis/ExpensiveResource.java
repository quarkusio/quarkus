package io.quarkus.it.cache.redis;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

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
        ExpensiveResponse response = new ExpensiveResponse();
        response.setResult(keyElement1 + " " + keyElement2 + " " + keyElement3 + " too!");
        return response;
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

    public static class ExpensiveResponse {

        private String result;

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }
    }
}
