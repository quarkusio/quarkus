package io.quarkus.it.keycloak;

import java.security.BasicPermission;

public class CustomPermission extends BasicPermission {

    public CustomPermission(String name) {
        super(name);
    }
}
