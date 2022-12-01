package io.quarkus.it.rest.client.wronghost;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(baseUri = "https://wrong.host.badssl.com/", configKey = "wrong-host")
public interface WrongHostClient {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Retry(delay = 1000)
    Response invoke();
}
