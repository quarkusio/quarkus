package io.quarkus.it.management;

import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.vertx.http.ManagementInterface;
import io.vertx.ext.web.Router;

@Path("/service")
public class GreetingResource {

    @GET
    @Path("/hello")
    public String hello() {
        return "hello";
    }

    @GET
    @Path("/goodbye")
    public String goodbye() {
        return "goodbye";
    }

    public void initManagement(@Observes ManagementInterface mi) {
        mi.router().get("/admin").handler(rc -> rc.response().end("admin it is"));
    }

    public void initMain(@Observes Router router) {
        router.get("/main").handler(rc -> rc.response().end("main"));
    }
}
