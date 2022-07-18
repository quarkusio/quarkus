package io.quarkus.test.security;

import java.security.Principal;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.interceptor.Interceptor;

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
