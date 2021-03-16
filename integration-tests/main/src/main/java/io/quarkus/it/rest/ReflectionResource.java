package io.quarkus.it.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("/reflection")
public class ReflectionResource {

    @GET
    @Path("/simpleClassName")
    public String getSimpleClassName(@QueryParam("className") String className) {
        try {
            Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
            return clazz.getSimpleName();
        } catch (ClassNotFoundException e) {
            return "FAILED";
        }
    }
}
