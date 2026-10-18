package io.quarkus.it.resteasy.reactive.elytron;

import java.security.BasicPermission;

public class ManagerPermission extends BasicPermission {
    public ManagerPermission(String name) {
        super(name);
    }

}
