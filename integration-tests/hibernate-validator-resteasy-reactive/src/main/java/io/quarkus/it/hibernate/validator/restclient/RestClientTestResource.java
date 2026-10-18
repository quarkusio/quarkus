package io.quarkus.it.hibernate.validator.restclient;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/hibernate-validator/test-rest-client")
public class RestClientTestResource {

    @Inject
    @RestClient
    RestClientInterface restClient;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public RestClientEntity doSomething() {
        RestClientEntity entity = new RestClientEntity(9, "aaa");
        try {
            entity = restClient.doSomething(entity);
            throw new IllegalStateException(
                    "This point should not be reached. Validation should fail on the rest client call.");
        } catch (ConstraintViolationException e) {
            return entity;
        }
    }
}
