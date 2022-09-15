package org.jboss.resteasy.reactive.server.vertx.test.simple;

import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.core.Context;
import java.util.Objects;

@RequestScoped
public class HelloService {

    @Context
    RoutingContext context;

    public String sayHello() {
        Objects.requireNonNull(context);
        return "Hello";
    }
}
