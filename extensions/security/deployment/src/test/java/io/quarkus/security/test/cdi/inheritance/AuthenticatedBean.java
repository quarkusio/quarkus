package io.quarkus.security.test.cdi.inheritance;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.Authenticated;

@ApplicationScoped
@Authenticated
public class AuthenticatedBean {

    public String ping() {
        return AuthenticatedBean.class.getSimpleName();
    }
}
