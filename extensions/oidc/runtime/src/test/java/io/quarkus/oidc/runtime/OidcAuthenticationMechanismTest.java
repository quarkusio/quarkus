package io.quarkus.oidc.runtime;

import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.quarkus.security.identity.IdentityProviderManager;
import io.vertx.ext.web.RoutingContext;

public class OidcAuthenticationMechanismTest {

    @Mock
    private DefaultTenantConfigResolver resolver;

    @Mock
    private RoutingContext context;

    @Mock
    private IdentityProviderManager identityProviderManager;

    @InjectMocks
    private OidcAuthenticationMechanism mechanism = new OidcAuthenticationMechanism();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldNotCheckWebAppInSync() {
        whenAuthenticate();
        thenResolverIsNotCalled();
    }

    private void whenAuthenticate() {
        mechanism.authenticate(context, identityProviderManager);
    }

    private void thenResolverIsNotCalled() {
        verifyNoInteractions(resolver);
    }
}
