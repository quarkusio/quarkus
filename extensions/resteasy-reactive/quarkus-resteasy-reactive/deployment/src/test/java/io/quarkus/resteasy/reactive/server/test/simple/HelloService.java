package io.quarkus.resteasy.reactive.server.test.simple;

import java.util.Objects;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import io.vertx.ext.web.RoutingContext;

@RequestScoped
public class HelloService {

    @Inject
    RoutingContext context;

    public String sayHello() {
        Objects.requireNonNull(context);
        return "Hello";
    }
}
