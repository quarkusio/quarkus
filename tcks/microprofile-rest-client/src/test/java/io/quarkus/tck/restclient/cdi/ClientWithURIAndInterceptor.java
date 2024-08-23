package io.quarkus.tck.restclient.cdi;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.tck.providers.ReturnWithURLRequestFilter;

@Path("/hello")
@RegisterProvider(ReturnWithURLRequestFilter.class)
@RegisterRestClient(baseUri = "http://localhost:5017/myBaseUri")
public interface ClientWithURIAndInterceptor {
    @GET
    @Loggable
    String get();

    @GET
    String getNoInterceptor();
}
