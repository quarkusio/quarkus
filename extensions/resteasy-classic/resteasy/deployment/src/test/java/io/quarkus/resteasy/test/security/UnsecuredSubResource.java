package io.quarkus.resteasy.test.security;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class UnsecuredSubResource {
    @GET
    @Path("/subMethod")
    public String subMethod() {
        return "subMethod";
    }
}
