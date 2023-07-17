package io.quarkus.test.security.jwt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.security.TestSecurityIdentityAugmentor;

@ApplicationScoped
public class JwtTestSecurityIdentityAugmentorProducer {

    @Produces
    @Unremovable
    public TestSecurityIdentityAugmentor produce() {
        return new JwtTestSecurityIdentityAugmentor();
    }
}
