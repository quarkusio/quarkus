package io.quarkus.it.rest.client.selfsigned;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "self-signed")
public interface ExternalSelfSignedClient {

    @GET
    @Retry(delay = 1000)
    Response invoke();
}
