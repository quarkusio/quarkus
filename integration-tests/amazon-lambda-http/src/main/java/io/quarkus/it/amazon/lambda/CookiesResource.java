package io.quarkus.it.amazon.lambda;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

@Path("/cookies")
@Produces(MediaType.APPLICATION_JSON)
public class CookiesResource {

    @GET
    @Path("/one/{name}")
    public Response one(@PathParam("name") String name) {
        NewCookie cookie = buildCookie("cookie1", name);
        return Response.ok("{\"status\":\"ok\"}").cookie(cookie).build();
    }

    @GET
    @Path("/two/{name1}/{name2}")
    public Response two(@PathParam("name1") String name1, @PathParam("name2") String name2) {
        NewCookie cookie1 = buildCookie("cookie1", name1);
        NewCookie cookie2 = buildCookie("cookie2", name2);
        return Response.ok("{\"status\":\"ok\"}").cookie(cookie1, cookie2).build();
    }

    private static NewCookie buildCookie(String name, String value) {
        return new NewCookie.Builder(name)
                .value(value)
                .path("/")
                .maxAge(3600)
                .secure(true)
                .httpOnly(true)
                .sameSite(NewCookie.SameSite.LAX)
                .build();
    }
}
