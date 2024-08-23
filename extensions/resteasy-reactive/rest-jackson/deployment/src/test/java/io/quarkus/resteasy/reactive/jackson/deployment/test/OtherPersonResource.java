package io.quarkus.resteasy.reactive.jackson.deployment.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.resteasy.reactive.jackson.DisableSecureSerialization;

@Path("other")
public class OtherPersonResource extends AbstractPersonResource {

    @GET
    @DisableSecureSerialization
    @Path("no-security")
    public Person nonSecurityPerson() {
        return abstractPerson();
    }
}
