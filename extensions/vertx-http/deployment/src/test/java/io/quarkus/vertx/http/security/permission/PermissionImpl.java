package io.quarkus.vertx.http.security.permission;

import java.security.BasicPermission;

public final class PermissionImpl extends BasicPermission {

    public PermissionImpl(String name, Object arg1) {
        super(name);
    }
}
