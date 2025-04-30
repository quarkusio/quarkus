package io.quarkus.security.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.identity.request.BaseAuthenticationRequest;
import io.smallrye.mutiny.Uni;

class QuarkusIdentityProviderManagerImplTest {

    @Test
    void testIdentityProviderPriority() {
        IdentityProviderManager identityProviderManager = QuarkusIdentityProviderManagerImpl.builder()
                .addProvider(new TestIdentityProviderSystemLastPriority())
                .addProvider(new TestIdentityProviderUserLastPriority())
                .addProvider(new TestIdentityProviderUserFirstPriority())
                .addProvider(new TestIdentityProviderSystemFirstPriority())
                .addProvider(new AnonymousIdentityProvider())
                .setBlockingExecutor(Executors.newSingleThreadExecutor())
                .build();

        SecurityIdentity identity = identityProviderManager.authenticateBlocking(new TestAuthenticationRequest());

        assertEquals(new QuarkusPrincipal("Bob"), identity.getPrincipal());
    }

    @Test
    void testIdentityProviderAugmentOnlyOnce() {
        TestSecurityAugmentor augmentor = spy(new TestSecurityAugmentor());
        IdentityProviderManager identityProviderManager = QuarkusIdentityProviderManagerImpl.builder()
                .addProvider(new TestIdentityProviderSystemFirstPriorityNoop())
                .addProvider(new TestIdentityProviderSystemLastPriorityUser())
                .addProvider(new AnonymousIdentityProvider())
                .addSecurityIdentityAugmentor(augmentor)
                .setBlockingExecutor(Executors.newSingleThreadExecutor()).build();
        SecurityIdentity identity = identityProviderManager.authenticateBlocking(new TestAuthenticationRequest());
        assertEquals(new QuarkusPrincipal("Bob"), identity.getPrincipal());
        assertTrue(identity.getRoles().contains("role"));
        verify(augmentor, times(1)).augment(any(), any());
    }

    static class TestAuthenticationRequest extends BaseAuthenticationRequest {
    }

    abstract static class TestIdentityProvider implements IdentityProvider<TestAuthenticationRequest> {
        @Override
        public Class<TestAuthenticationRequest> getRequestType() {
            return TestAuthenticationRequest.class;
        }

        @Override
        public Uni<SecurityIdentity> authenticate(TestAuthenticationRequest request, AuthenticationRequestContext context) {
            throw new AuthenticationFailedException(getClass().getSimpleName());
        }
    }

    static class TestIdentityProviderUserFirstPriority extends TestIdentityProvider {
        @Override
        public int priority() {
            return Integer.MAX_VALUE;
        }

        @Override
        public Uni<SecurityIdentity> authenticate(TestAuthenticationRequest request, AuthenticationRequestContext context) {
            SecurityIdentity identity = QuarkusSecurityIdentity.builder()
                    .setPrincipal(new QuarkusPrincipal("Bob"))
                    .build();
            return Uni.createFrom().item(identity);
        }
    }

    static class TestIdentityProviderUserLastPriority extends TestIdentityProvider {
    }

    static class TestIdentityProviderSystemFirstPriority extends TestIdentityProvider {
        @Override
        public int priority() {
            return SYSTEM_FIRST;
        }
    }

    static class TestIdentityProviderSystemLastPriority extends TestIdentityProvider {
        @Override
        public int priority() {
            return SYSTEM_LAST;
        }
    }

    static class TestIdentityProviderSystemFirstPriorityNoop extends TestIdentityProviderSystemLastPriority {
        @Override
        public Uni<SecurityIdentity> authenticate(TestAuthenticationRequest request, AuthenticationRequestContext context) {
            return Uni.createFrom().nullItem();
        }
    }

    static class TestIdentityProviderSystemLastPriorityUser extends TestIdentityProviderUserFirstPriority {
        @Override
        public int priority() {
            return SYSTEM_LAST;
        }
    }

    private static class TestSecurityAugmentor implements SecurityIdentityAugmentor {
        @Override
        public Uni<SecurityIdentity> augment(SecurityIdentity securityIdentity,
                AuthenticationRequestContext authenticationRequestContext) {
            return Uni.createFrom().item(QuarkusSecurityIdentity.builder(securityIdentity).addRole("role").build());
        }
    }
}
