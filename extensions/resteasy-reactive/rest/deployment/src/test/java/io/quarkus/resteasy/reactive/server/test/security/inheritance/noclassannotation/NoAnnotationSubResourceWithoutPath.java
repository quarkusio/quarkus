package io.quarkus.resteasy.reactive.server.test.security.inheritance.noclassannotation;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SECURED_SUB_RESOURCE_ENDPOINT_PATH;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.vertx.core.json.JsonObject;

public class NoAnnotationSubResourceWithoutPath {

    private final String subResourcePath;

    public NoAnnotationSubResourceWithoutPath(String subResourcePath) {
        this.subResourcePath = subResourcePath;
    }

    @POST
    public String post(JsonObject array) {
        return subResourcePath;
    }

    @RolesAllowed("admin")
    @Path(SECURED_SUB_RESOURCE_ENDPOINT_PATH)
    @POST
    public String securedPost(JsonObject array) {
        return subResourcePath + SECURED_SUB_RESOURCE_ENDPOINT_PATH;
    }

}
