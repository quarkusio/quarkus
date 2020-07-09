package io.quarkus.security.test.cdi;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;

import io.quarkus.security.Authenticated;

@ApplicationScoped
@Authenticated
@RolesAllowed("admin")
public class InvalidBean {
}
