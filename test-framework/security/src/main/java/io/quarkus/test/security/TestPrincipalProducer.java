package io.quarkus.test.security;

import java.security.Principal;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

import io.quarkus.security.identity.SecurityIdentity;

@Alternative
@Priority(Interceptor.Priority.LIBRARY_AFTER)
@ApplicationScoped
public class TestPrincipalProducer {

    @Inject
    SecurityIdentity testIdentity;

    @Produces
    @RequestScoped
    public Principal getTestIdentity() {
        return testIdentity.getPrincipal();
    }
}
