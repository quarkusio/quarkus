package io.quarkus.it.resteasy.elytron;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.PermissionsAllowed;

@ApplicationScoped
@PermissionsAllowed({ "see:detail", "see:all" })
public class BeanSecuredWithPermissions {

    public String getDetail() {
        return "detail";
    }

    @PermissionsAllowed("create:all")
    public String create() {
        return "created";
    }

    @PermissionsAllowed("modify")
    public String modify() {
        return "modified";
    }

}
