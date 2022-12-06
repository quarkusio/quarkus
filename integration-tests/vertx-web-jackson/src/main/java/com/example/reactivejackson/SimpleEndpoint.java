package com.example.reactivejackson;

import static io.quarkus.vertx.web.Route.HttpMethod.GET;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
@RouteBase(path = "/simple")
public class SimpleEndpoint {

    @Route(path = "person", methods = GET)
    public Uni<Person> get() {
        return Uni.createFrom().item(new Person("Foo", null));
    }

}
