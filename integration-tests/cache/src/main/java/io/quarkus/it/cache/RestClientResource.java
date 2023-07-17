package io.quarkus.it.cache;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

@Path("rest-client")
public class RestClientResource {

    @RestClient
    SunriseRestClient sunriseRestClient;

    @GET
    @Path("time/{city}")
    public String getSunriseTime(@RestPath String city, @RestQuery String date) {
        return sunriseRestClient.getSunriseTime(city, date);
    }

    @GET
    @Path("invocations")
    public Integer getSunriseTimeInvocations() {
        return sunriseRestClient.getSunriseTimeInvocations();
    }

    @DELETE
    @Path("invalidate/{city}")
    public void invalidate(@RestPath String city, @RestQuery String notPartOfTheCacheKey, @RestQuery String date) {
        sunriseRestClient.invalidate(city, notPartOfTheCacheKey, date);
    }

    @DELETE
    @Path("invalidate")
    public void invalidateAll() {
        sunriseRestClient.invalidateAll();
    }
}
