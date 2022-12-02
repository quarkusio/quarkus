package io.quarkus.resteasy.reactive.server.test.simple;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

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
