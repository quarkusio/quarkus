package io.quarkus.it.rest.client.selfsigned;

import javax.ws.rs.GET;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(baseUri = "https://self-signed.badssl.com/", configKey = "self-signed")
public interface ExternalSelfSignedClient {

    @GET
    @Retry(delay = 1000)
    Response invoke();
}
