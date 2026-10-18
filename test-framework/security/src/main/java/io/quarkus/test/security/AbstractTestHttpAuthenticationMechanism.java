package io.quarkus.test.security;

import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.ROUTING_CONTEXT_ATTRIBUTE;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

abstract class AbstractTestHttpAuthenticationMechanism implements HttpAuthenticationMechanism {

    @Inject
    TestIdentityAssociation testIdentityAssociation;

    @Inject
    BlockingSecurityExecutor blockingSecurityExecutor;

    protected volatile String authMechanism = null;
    protected volatile List<Instance<? extends SecurityIdentityAugmentor>> augmentors = null;

    @PostConstruct
    public void check() {
        if (LaunchMode.current() != LaunchMode.TEST) {
            //paranoid check
            throw new RuntimeException("TestAuthController can only be used in tests");
        }
    }

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext event, IdentityProviderManager identityProviderManager) {
        var identity = Uni.createFrom().item(testIdentityAssociation.getTestIdentity());
        if (augmentors != null && testIdentityAssociation.getTestIdentity() != null) {
            var requestContext = new AuthenticationRequestContext() {
                @Override
                public Uni<SecurityIdentity> runBlocking(Supplier<SecurityIdentity> supplier) {
                    return blockingSecurityExecutor.executeBlocking(supplier);
                }
            };
            var requestAttributes = Map.<String, Object> of(ROUTING_CONTEXT_ATTRIBUTE, event);
            for (var augmentor : augmentors) {
                identity = identity.flatMap(i -> augmentor.get().augment(i, requestContext, requestAttributes));
            }
        }
        return identity;
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
    public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
        return authMechanism == null ? Uni.createFrom().nullItem()
                : Uni.createFrom().item(new HttpCredentialTransport(HttpCredentialTransport.Type.TEST_SECURITY, authMechanism));
    }

    void setAuthMechanism(String authMechanism) {
        this.authMechanism = authMechanism;
    }

    void setSecurityIdentityAugmentors(List<Instance<? extends SecurityIdentityAugmentor>> augmentors) {
        this.augmentors = augmentors;
    }
}
