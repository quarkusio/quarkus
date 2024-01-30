package io.quarkus.oidc.proxy;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.oidc.token.propagation.AccessToken;
import io.smallrye.mutiny.Uni;

@RegisterRestClient(configKey = "service-api-client")
@AccessToken
public interface ServiceApiClient {

    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    Uni<String> getName();
}
