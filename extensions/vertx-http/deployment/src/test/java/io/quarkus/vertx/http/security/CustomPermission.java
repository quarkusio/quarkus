package io.quarkus.vertx.http.security;

import java.security.BasicPermission;

public class CustomPermission extends BasicPermission {
    public CustomPermission(String name) {
        super(name);
    }
}
