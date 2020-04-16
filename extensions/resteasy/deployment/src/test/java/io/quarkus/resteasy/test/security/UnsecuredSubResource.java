package io.quarkus.resteasy.test.security;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
