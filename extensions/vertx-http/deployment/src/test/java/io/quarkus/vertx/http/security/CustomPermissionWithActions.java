package io.quarkus.vertx.http.security;

import java.security.Permission;

import io.quarkus.security.StringPermission;

public class CustomPermissionWithActions extends Permission {

    private final Permission delegate;

    public CustomPermissionWithActions(String name, String[] actions) {
        super(name);
        this.delegate = new StringPermission(name, actions);
    }

    @Override
    public boolean implies(Permission permission) {
        if (permission instanceof CustomPermissionWithActions) {
            return delegate.implies(((CustomPermissionWithActions) permission).delegate);
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String getActions() {
        return delegate.getActions();
    }
}
