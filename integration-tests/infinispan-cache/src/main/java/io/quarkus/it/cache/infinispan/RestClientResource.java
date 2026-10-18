package io.quarkus.it.cache.infinispan;

import java.util.Set;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.HttpHeaders;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.runtime.BlockingOperationControl;
import io.smallrye.mutiny.Uni;

@Path("rest-client")
public class RestClientResource {

    @RestClient
    SunriseRestClient sunriseRestClient;

    @Inject
    HttpHeaders headers; // used in order to make sure that @RequestScoped beans continue to work despite the cache coming into play

    @GET
    @Path("time/{city}")
    public RestResponse<String> getSunriseTime(@RestPath String city, @RestQuery String date) {
        Set<String> incomingHeadersBeforeRestCall = headers.getRequestHeaders().keySet();
        String restResponse = sunriseRestClient.getSunriseTime(city, date);
        Set<String> incomingHeadersAfterRestCall = headers.getRequestHeaders().keySet();
        return RestResponse.ResponseBuilder
                .ok(restResponse)
                .header("before", String.join(", ", incomingHeadersBeforeRestCall))
                .header("after", String.join(", ", incomingHeadersAfterRestCall))
                .header("blockingAllowed", BlockingOperationControl.isBlockingAllowed())
                .build();
    }

    @GET
    @Path("async/time/{city}")
    public Uni<RestResponse<String>> getAsyncSunriseTime(@RestPath String city, @RestQuery String date) {
        Set<String> incomingHeadersBeforeRestCall = headers.getRequestHeaders().keySet();
        return sunriseRestClient.getAsyncSunriseTime(city, date).onItem().transform(new Function<>() {
            @Override
            public RestResponse<String> apply(String restResponse) {
                Set<String> incomingHeadersAfterRestCall = headers.getRequestHeaders().keySet();
                return RestResponse.ResponseBuilder
                        .ok(restResponse)
                        .header("before", String.join(", ", incomingHeadersBeforeRestCall))
                        .header("after", String.join(", ", incomingHeadersAfterRestCall))
                        .header("blockingAllowed", BlockingOperationControl.isBlockingAllowed())
                        .build();
            }
        });
    }

    @GET
    @Path("invocations")
    public Integer getSunriseTimeInvocations() {
        return sunriseRestClient.getSunriseTimeInvocations();
    }

    @DELETE
    @Path("invalidate/{city}")
    public Uni<RestResponse<Void>> invalidate(@RestPath String city, @RestQuery String notPartOfTheCacheKey,
            @RestQuery String date) {
        return sunriseRestClient.invalidate(city, notPartOfTheCacheKey, date).onItem().transform(
                new Function<>() {
                    @Override
                    public RestResponse<Void> apply(Void unused) {
                        return RestResponse.ResponseBuilder.<Void> create(RestResponse.Status.NO_CONTENT)
                                .header("blockingAllowed", BlockingOperationControl.isBlockingAllowed())
                                .header("incoming", String.join(", ", headers.getRequestHeaders().keySet()))
                                .build();
                    }
                });
    }

    @DELETE
    @Path("invalidate")
    public void invalidateAll() {
        sunriseRestClient.invalidateAll();
    }
}
