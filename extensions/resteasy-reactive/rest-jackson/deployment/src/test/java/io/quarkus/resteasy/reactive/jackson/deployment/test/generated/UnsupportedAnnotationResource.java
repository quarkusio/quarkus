package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.smallrye.common.annotation.NonBlocking;

@Path("/unsupported")
@NonBlocking
public class UnsupportedAnnotationResource {

    // --- @JsonAutoDetect ---

    @GET
    @Path("/auto-detect")
    public AutoDetectBean getAutoDetect() {
        return new AutoDetectBean("hello", 42);
    }
}
