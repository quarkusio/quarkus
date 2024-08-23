package io.quarkus.test.security.oidc;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.security.TestSecurityIdentityAugmentor;

@ApplicationScoped
public class OidcTestSecurityIdentityAugmentorProducer {

    @Inject
    @ConfigProperty(name = "quarkus.oidc.token.issuer")
    Optional<String> issuer;

    @Produces
    @Unremovable
    public TestSecurityIdentityAugmentor produce() {
        return new OidcTestSecurityIdentityAugmentor(issuer);
    }
}
