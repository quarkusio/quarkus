package io.quarkus.it.cache.infinispan;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

@ApplicationScoped
@Path("sunrise")
public class SunriseRestServerResource {

    private final AtomicInteger invocations = new AtomicInteger(0);

    @Inject
    ClientRequestService requestService;

    @GET
    @Path("time/{city}")
    public String getSunriseTime(@RestPath String city, @RestQuery String date) {
        invocations.incrementAndGet();
        requestService.setData(city);
        return "2020-12-20T10:15:30";
    }

    @GET
    @Path("invocations")
    public Integer getSunriseTimeInvocations() {
        requestService.setData("invocations");
        return invocations.get();
    }

    @DELETE
    @Path("invalidate/{city}")
    public void invalidate(@RestPath String city, @RestQuery String notPartOfTheCacheKey, @RestQuery String date) {
        // Do nothing. We only need to test the caching annotation on the client side.
        requestService.setData("invalidate " + city);
    }

    @DELETE
    @Path("invalidate")
    public void invalidateAll() {
        // Do nothing. We only need to test the caching annotation on the client side.
        requestService.setData("invalidate all");
    }
}
