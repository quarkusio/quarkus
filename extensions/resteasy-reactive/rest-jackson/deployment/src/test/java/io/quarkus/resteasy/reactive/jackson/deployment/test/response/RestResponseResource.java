package io.quarkus.resteasy.reactive.jackson.deployment.test.response;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestResponse;

@Path("")
public class RestResponseResource {

    @GET
    @Path("json")
    public JsonSomething getJson() {
        return new JsonSomething("Stef", "Epardaud");
    }

    @GET
    @Path("rest-response-json")
    public RestResponse<JsonSomething> getRestResponseJson() {
        return RestResponse.ok(new JsonSomething("Stef", "Epardaud"));
    }
}
