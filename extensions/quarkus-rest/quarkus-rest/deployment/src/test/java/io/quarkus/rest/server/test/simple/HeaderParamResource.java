package io.quarkus.rest.server.test.simple;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;

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
