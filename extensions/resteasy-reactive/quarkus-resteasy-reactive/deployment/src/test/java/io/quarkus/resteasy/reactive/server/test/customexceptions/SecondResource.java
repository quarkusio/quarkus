package io.quarkus.resteasy.reactive.server.test.customexceptions;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("second")
public class SecondResource {

    @GET
    @Produces("text/plain")
    public String throwsMyException() {
        throw new MyException();
    }

    @GET
    @Path("other")
    @Produces("text/plain")
    public String throwsMyOtherException() {
        throw new MyOtherException();
    }

    @GET
    @Path("uni")
    @Produces("text/plain")
    public String throwsUniException() {
        throw new UniException();
    }

    @GET
    @Path("extendsUni")
    @Produces("text/plain")
    public String throwsExtendsUniException() {
        throw new ExtendsUniException();
    }
}
