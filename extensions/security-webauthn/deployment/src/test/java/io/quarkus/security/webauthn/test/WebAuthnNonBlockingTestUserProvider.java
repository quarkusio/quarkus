package io.quarkus.security.webauthn.test;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.resteasy.reactive.server.core.BlockingOperationSupport;

import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.auth.webauthn.Authenticator;

/**
 * This UserProvider stores and updates the credentials in the callback endpoint, and checks that it's non-blocking
 */
@ApplicationScoped
public class WebAuthnNonBlockingTestUserProvider extends WebAuthnTestUserProvider {
    @Override
    public Uni<List<Authenticator>> findWebAuthnCredentialsByCredID(String credId) {
        assertBlockingNotAllowed();
        return super.findWebAuthnCredentialsByCredID(credId);
    }

    @Override
    public Uni<List<Authenticator>> findWebAuthnCredentialsByUserName(String userId) {
        assertBlockingNotAllowed();
        return super.findWebAuthnCredentialsByUserName(userId);
    }

    @Override
    public Uni<Void> updateOrStoreWebAuthnCredentials(Authenticator authenticator) {
        assertBlockingNotAllowed();
        return super.updateOrStoreWebAuthnCredentials(authenticator);
    }

    private void assertBlockingNotAllowed() {
        // allow this being used in the tests
        if (TestUtil.isTestThread())
            return;
        if (BlockingOperationSupport.isBlockingAllowed())
            throw new RuntimeException("Blocking should not be allowed");
    }
}
