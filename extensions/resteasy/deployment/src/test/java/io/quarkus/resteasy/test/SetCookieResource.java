package io.quarkus.resteasy.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import org.jboss.resteasy.spi.HttpResponse;

@Path("/set-cookies")
public class SetCookieResource {

    @GET
    public void setCookies(@Context HttpResponse response) {
        response.getOutputHeaders().add(HttpHeaders.SET_COOKIE, "c1=c1");
        response.getOutputHeaders().add(HttpHeaders.SET_COOKIE, "c2=c2");
        response.getOutputHeaders().add(HttpHeaders.SET_COOKIE, "c3=c3");
    }
}
