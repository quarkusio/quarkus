package io.quarkus.resteasy.test.security.inheritance.classrolesallowed;

import jakarta.ws.rs.POST;

import io.vertx.core.json.JsonObject;

public class ClassRolesAllowedSubResourceWithoutPath {

    private final String subResourcePath;

    public ClassRolesAllowedSubResourceWithoutPath(String subResourcePath) {
        this.subResourcePath = subResourcePath;
    }

    @POST
    public String post(JsonObject array) {
        return subResourcePath;
    }

}
