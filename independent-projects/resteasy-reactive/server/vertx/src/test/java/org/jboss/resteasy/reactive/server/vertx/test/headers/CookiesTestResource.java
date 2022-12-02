package org.jboss.resteasy.reactive.server.vertx.test.headers;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

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
