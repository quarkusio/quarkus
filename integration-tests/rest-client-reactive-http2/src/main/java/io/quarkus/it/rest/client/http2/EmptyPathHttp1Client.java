package io.quarkus.it.rest.client.http2;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * The same empty-path client over HTTP/1.1.
 */
@Path("")
@RegisterRestClient(configKey = "empty-path-http1-client")
public interface EmptyPathHttp1Client {

    @GET
    String root();
}
