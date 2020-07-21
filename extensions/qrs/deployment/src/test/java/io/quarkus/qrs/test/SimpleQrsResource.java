package io.quarkus.qrs.test;

import io.quarkus.qrs.Blocking;
import io.quarkus.runtime.BlockingOperationControl;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/simple")
public class SimpleQrsResource {

    @GET
    public String get() {
        return "GET";
    }

    @GET
    @Path("{id}")
    public String get(@PathParam("id") String id) {
        return "GET:" + id;
    }

    @GET
    @Path("params/{p}")
    public String params(@PathParam("p") String p, 
                         @QueryParam("q") String q,
                         @HeaderParam("h") String h) {
        return "params: p: " + p+", q: "+q+", h: "+h;
    }

    @POST
    public String post() {
        return "POST";
    }

    @GET
    @Path("/person")
    @Produces(MediaType.APPLICATION_JSON)
    public Person getPerson() {
        Person person = new Person();
        person.setFirst("Bob");
        person.setLast("Builder");
        return person;
    }

    @GET
    @Path("/blocking")
    @Blocking
    public String blocking() {
        return String.valueOf(BlockingOperationControl.isBlockingAllowed());
    }
}
