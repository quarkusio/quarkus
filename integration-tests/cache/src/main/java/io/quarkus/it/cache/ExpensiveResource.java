package io.quarkus.it.cache;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;

@ApplicationScoped
@Path("/expensive-resource")
public class ExpensiveResource {

    private int invocations;

    @GET
    @Path("/{keyElement1}/{keyElement2}/{keyElement3}")
    @CacheResult(cacheName = "expensiveResourceCache", lockTimeout = 5000)
    public ExpensiveResponse getExpensiveResponse(@PathParam("keyElement1") @CacheKey String keyElement1,
            @PathParam("keyElement2") @CacheKey String keyElement2, @PathParam("keyElement3") @CacheKey String keyElement3,
            @QueryParam("foo") String foo) {
        invocations++;
        ExpensiveResponse response = new ExpensiveResponse();
        response.setResult(keyElement1 + " " + keyElement2 + " " + keyElement3 + " too!");
        return response;
    }

    @GET
    @Path("/invocations")
    public int getInvocations() {
        return invocations;
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
