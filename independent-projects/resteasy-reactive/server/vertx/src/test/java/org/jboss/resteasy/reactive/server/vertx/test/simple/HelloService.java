package org.jboss.resteasy.reactive.server.vertx.test.simple;

import io.vertx.ext.web.RoutingContext;
import java.util.Objects;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class HelloService {

    @Inject
    RoutingContext context;

    public String sayHello() {
        Objects.requireNonNull(context);
        return "Hello";
    }
}
