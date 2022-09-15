package io.quarkus.test.security;

import java.util.Collections;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class TestHttpAuthenticationMechanism implements HttpAuthenticationMechanism {

    @Inject
    TestIdentityAssociation testIdentityAssociation;

    @PostConstruct
    public void check() {
        if (LaunchMode.current() != LaunchMode.TEST) {
            //paranoid check
            throw new RuntimeException("TestAuthController can only be used in tests");
        }
    }

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
        return Uni.createFrom().item(testIdentityAssociation.getTestIdentity());
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        return Uni.createFrom().nullItem();
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return Collections.emptySet();
    }

    @Override
    public HttpCredentialTransport getCredentialTransport() {
        return null;
    }
}
