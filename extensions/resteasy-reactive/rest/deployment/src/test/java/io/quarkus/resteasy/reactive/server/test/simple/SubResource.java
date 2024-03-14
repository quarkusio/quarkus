package io.quarkus.resteasy.reactive.server.test.simple;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

public class SubResource {

    @GET
    public String sub() {
        return "sub";
    }

    @GET
    @Path("otherSub")
    public String otherPath() {
        return "otherSub";
    }

    @Path("patch/text")
    @PATCH
    @Consumes(MediaType.TEXT_PLAIN)
    public String patchWithTextPlain(String patch) {
        return "test-value: " + patch;
    }
}
