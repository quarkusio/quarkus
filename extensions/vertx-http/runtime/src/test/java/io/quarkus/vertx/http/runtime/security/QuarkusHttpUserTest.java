package io.quarkus.vertx.http.runtime.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuarkusHttpUserTest {

    @Mock
    private SecurityIdentity securityIdentity;

    @Mock
    private Principal principal;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RoutingContext routingContext;

    @Mock
    private IdentityProviderManager identityProviderManager;

    private QuarkusHttpUser user;

    @BeforeEach
    void setUp() {
        when(securityIdentity.getPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("testuser");
        user = new QuarkusHttpUser(securityIdentity);
    }

    @Test
    void principalReturnsJsonObjectWithUsername() {
        JsonObject result = user.principal();
        assertEquals("testuser", result.getString("username"));
    }

    @Test
    void attributesReturnsSameAsPrincipal() {
        JsonObject attributes = user.attributes();
        assertEquals("testuser", attributes.getString("username"));
    }

    @Test
    void getSecurityIdentityReturnsWrappedIdentity() {
        assertSame(securityIdentity, user.getSecurityIdentity());
    }

    @Test
    void mergeWithNullReturnsThis() {
        User result = user.merge(null);
        assertSame(user, result);
    }

    @Test
    void mergeWithOtherMergesOtherPrincipal() {
        User other = mock(User.class);
        JsonObject otherPrincipal = new JsonObject().put("email", "test@example.com");
        when(other.principal()).thenReturn(otherPrincipal);

        User result = user.merge(other);

        assertSame(user, result);
        verify(other).principal();
    }

    @Test
    void getSecurityIdentityBlockingWithExistingUserReturnsIdentity() {
        when(routingContext.user()).thenReturn(user);

        SecurityIdentity result = QuarkusHttpUser.getSecurityIdentityBlocking(routingContext, identityProviderManager);

        assertSame(securityIdentity, result);
    }

    @Test
    void getSecurityIdentityBlockingWithNoUserNoDeferredNoIpmReturnsNull() {
        when(routingContext.user()).thenReturn(null);
        when(routingContext.get(QuarkusHttpUser.DEFERRED_IDENTITY_KEY)).thenReturn(null);

        SecurityIdentity result = QuarkusHttpUser.getSecurityIdentityBlocking(routingContext, null);

        assertNull(result);
    }

    @Test
    void getSecurityIdentityUniWithDeferredReturnsIt() {
        Uni<SecurityIdentity> deferred = Uni.createFrom().item(securityIdentity);
        when(routingContext.get(QuarkusHttpUser.DEFERRED_IDENTITY_KEY)).thenReturn(deferred);

        Uni<SecurityIdentity> result = QuarkusHttpUser.getSecurityIdentity(routingContext, identityProviderManager);

        assertSame(deferred, result);
    }

    @Test
    void getSecurityIdentityUniWithExistingUserWrapsIdentity() {
        when(routingContext.get(QuarkusHttpUser.DEFERRED_IDENTITY_KEY)).thenReturn(null);
        when(routingContext.user()).thenReturn(user);

        Uni<SecurityIdentity> result = QuarkusHttpUser.getSecurityIdentity(routingContext, identityProviderManager);

        assertSame(securityIdentity, result.await().indefinitely());
    }

    @Test
    void getSecurityIdentityUniWithNoUserNoIpmReturnsNullItem() {
        when(routingContext.get(QuarkusHttpUser.DEFERRED_IDENTITY_KEY)).thenReturn(null);
        when(routingContext.user()).thenReturn(null);

        Uni<SecurityIdentity> result = QuarkusHttpUser.getSecurityIdentity(routingContext, null);

        assertNull(result.await().indefinitely());
    }

    @Test
    void setIdentityWithSecurityIdentitySetsUserAndDeferredKey() {
        io.vertx.ext.web.impl.UserContextInternal userContext = mock(io.vertx.ext.web.impl.UserContextInternal.class);
        when(routingContext.userContext()).thenReturn(userContext);

        SecurityIdentity result = QuarkusHttpUser.setIdentity(securityIdentity, routingContext);

        assertSame(securityIdentity, result);
        verify(userContext).setUser(any(QuarkusHttpUser.class));
        verify(routingContext).put(eq(QuarkusHttpUser.DEFERRED_IDENTITY_KEY), any(Uni.class));
    }
}
