package io.quarkus.security.test.cdi.inheritance;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.PermissionsAllowed;

@ApplicationScoped
@PermissionsAllowed("read")
public class PermissionsAllowedBean {

    public String ping() {
        return PermissionsAllowedBean.class.getSimpleName();
    }
}
