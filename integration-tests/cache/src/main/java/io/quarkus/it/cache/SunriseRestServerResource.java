package io.quarkus.it.cache;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

@ApplicationScoped
@Path("sunrise")
public class SunriseRestServerResource {

    private int sunriseTimeInvocations;

    @GET
    @Path("time/{city}")
    public String getSunriseTime(@PathParam String city, @QueryParam String date) {
        sunriseTimeInvocations++;
        return "2020-12-20T10:15:30";
    }

    @GET
    @Path("invocations")
    public Integer getSunriseTimeInvocations() {
        return sunriseTimeInvocations;
    }

    @DELETE
    @Path("invalidate/{city}")
    public void invalidate(@PathParam String city, @QueryParam String notPartOfTheCacheKey, @QueryParam String date) {
        // Do nothing. We only need to test the caching annotation on the client side.
    }

    @DELETE
    @Path("invalidate")
    public void invalidateAll() {
        // Do nothing. We only need to test the caching annotation on the client side.
    }
}
