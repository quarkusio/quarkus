package io.quarkus.resteasy.reactive.server.test.security.inheritance.classdenyall;

import jakarta.ws.rs.POST;

import io.vertx.core.json.JsonObject;

public class ClassDenyAllSubResourceWithoutPath {

    private final String subResourcePath;

    public ClassDenyAllSubResourceWithoutPath(String subResourcePath) {
        this.subResourcePath = subResourcePath;
    }

    @POST
    public String post(JsonObject array) {
        return subResourcePath;
    }

}
