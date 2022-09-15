package org.jboss.resteasy.reactive.server.vertx.test.headers;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

@Path("/cookies")
public class CookiesTestResource {

    @POST
    @Path("/set-cookie")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response sameSite(@FormParam("cookie") String cookie) {
        return Response.status(200)
                .cookie(NewCookie.valueOf(cookie))
                .build();
    }
}
