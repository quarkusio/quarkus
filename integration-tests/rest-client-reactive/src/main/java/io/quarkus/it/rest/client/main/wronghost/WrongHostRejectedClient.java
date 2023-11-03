package io.quarkus.it.rest.client.main.wronghost;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(baseUri = "https://wrong.host.badssl.com/", configKey = "wrong-host-rejected")
public interface WrongHostRejectedClient {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    Response invoke();
}
