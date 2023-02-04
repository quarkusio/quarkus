package org.jboss.resteasy.reactive.server.vertx.test.customexceptions;

import static org.jboss.resteasy.reactive.server.vertx.test.ExceptionUtil.removeStackTrace;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("second")
public class SecondResource {

    @GET
    @Produces("text/plain")
    public String throwsMyException() {
        throw removeStackTrace(new MyException());
    }

    @GET
    @Path("other")
    @Produces("text/plain")
    public String throwsMyOtherException() {
        throw removeStackTrace(new MyOtherException());
    }

    @GET
    @Path("uni")
    @Produces("text/plain")
    public String throwsUniException() {
        throw removeStackTrace(new UniException());
    }

    @GET
    @Path("extendsUni")
    @Produces("text/plain")
    public String throwsExtendsUniException() {
        throw removeStackTrace(new ExtendsUniException());
    }
}
