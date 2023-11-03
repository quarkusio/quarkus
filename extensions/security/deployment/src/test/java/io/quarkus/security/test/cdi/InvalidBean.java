package io.quarkus.security.test.cdi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.Authenticated;

@ApplicationScoped
@Authenticated
@RolesAllowed("admin")
public class InvalidBean {
}
