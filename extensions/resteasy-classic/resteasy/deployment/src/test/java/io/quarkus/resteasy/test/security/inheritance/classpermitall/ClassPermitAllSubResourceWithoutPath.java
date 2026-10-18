package io.quarkus.resteasy.test.security.inheritance.classpermitall;

import jakarta.ws.rs.POST;

import io.vertx.core.json.JsonObject;

public class ClassPermitAllSubResourceWithoutPath {

    private final String subResourcePath;

    public ClassPermitAllSubResourceWithoutPath(String subResourcePath) {
        this.subResourcePath = subResourcePath;
    }

    @POST
    public String post(JsonObject array) {
        return subResourcePath;
    }

}
