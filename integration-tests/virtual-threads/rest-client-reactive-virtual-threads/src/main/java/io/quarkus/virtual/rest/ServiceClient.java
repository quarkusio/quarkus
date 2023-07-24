package io.quarkus.virtual.rest;

import jakarta.ws.rs.GET;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(baseUri = "http://localhost:8081/api")
public interface ServiceClient {

    @GET
    Greeting hello();

}
