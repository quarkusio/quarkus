package io.quarkus.it.rest.client.http2;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * A client whose request path is empty, so that the request goes to the root of its base URL, over HTTP/2.
 */
@Path("")
@RegisterRestClient(configKey = "empty-path-client")
public interface EmptyPathClient {

    @GET
    String root();
}
