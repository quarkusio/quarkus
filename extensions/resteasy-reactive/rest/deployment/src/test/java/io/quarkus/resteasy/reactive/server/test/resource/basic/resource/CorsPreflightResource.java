package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

public class CorsPreflightResource {
    public static final String TEST_PREFLIGHT_HEADER = "preflight-header-test";

    @Path("{any:.*}")
    @OPTIONS
    public Response preflight() {
        return Response.ok().allow(HttpMethod.GET, HttpMethod.POST, HttpMethod.OPTIONS, HttpMethod.HEAD)
                .header(TEST_PREFLIGHT_HEADER, "test").build();
    }
}
