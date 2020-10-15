package io.quarkus.rest.test.simple;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/new-params/{klass}/{regex:[^/]+}")
public class NewParamsRestResource {

    @GET
    @Path("{id}")
    public String get(String klass, String regex, String id) {
        return "GET:" + klass + ":" + regex + ":" + id;
    }
}
