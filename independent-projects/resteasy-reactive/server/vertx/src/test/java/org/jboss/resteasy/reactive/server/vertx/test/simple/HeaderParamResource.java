package org.jboss.resteasy.reactive.server.vertx.test.simple;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;

@Path("/ctor-header")
public class HeaderParamResource {

    private final String headerParamValue;

    public HeaderParamResource(@HeaderParam("h1") String headerParamValue) {
        this.headerParamValue = headerParamValue;
    }

    @GET
    public String get() {
        return headerParamValue;
    }
}
