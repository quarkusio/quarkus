package io.quarkus.resteasy.reactive.server.test.security;

import java.security.Permission;

import io.quarkus.arc.Arc;
import io.vertx.ext.web.RoutingContext;

public class CustomPermission extends Permission {

    public CustomPermission(String name) {
        super(name);
    }

    @Override
    public boolean implies(Permission permission) {
        var event = Arc.container().instance(RoutingContext.class).get();
        return "hello".equals(event.request().params().get("greeting"));
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getActions() {
        return null;
    }
}
