package io.quarkus.test.qute;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestQuery;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;

@Path("hello")
public class HelloResource {

    @Inject
    Template hello;

    @Inject
    Engine engine;

    @GET
    public String get(@RestQuery String name) {
        return hello.data("name", name).render();
    }

    @GET
    @Path("ping")
    public String ping() {
        return engine.getTemplate("ping").render();
    }

}
