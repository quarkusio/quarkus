package io.quarkus.resteasy.reactive.server.test.beanparam;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestPath;

@Path("fs")
public class FirstAndSecondResource {

    @Path("{first}/{second}")
    @GET
    public String firstAndSecond(Param param) {
        return param.first() + "-" + param.second();
    }

    public record Param(@RestPath String first, @RestPath String second) {

    }
}
