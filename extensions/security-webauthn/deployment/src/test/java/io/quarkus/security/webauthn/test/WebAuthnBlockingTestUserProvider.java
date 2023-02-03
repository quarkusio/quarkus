package io.quarkus.security.webauthn.test;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.resteasy.reactive.server.core.BlockingOperationSupport;

import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.auth.webauthn.Authenticator;

/**
 * This UserProvider stores and updates the credentials in the callback endpoint, but is blocking
 */
@ApplicationScoped
@Blocking
public class WebAuthnBlockingTestUserProvider extends WebAuthnTestUserProvider {
    @Override
    public Uni<List<Authenticator>> findWebAuthnCredentialsByCredID(String credId) {
        assertBlockingAllowed();
        return super.findWebAuthnCredentialsByCredID(credId);
    }

    @Override
    public Uni<List<Authenticator>> findWebAuthnCredentialsByUserName(String userId) {
        assertBlockingAllowed();
        return super.findWebAuthnCredentialsByUserName(userId);
    }

    @Override
    public Uni<Void> updateOrStoreWebAuthnCredentials(Authenticator authenticator) {
        assertBlockingAllowed();
        return super.updateOrStoreWebAuthnCredentials(authenticator);
    }

    private void assertBlockingAllowed() {
        if (!BlockingOperationSupport.isBlockingAllowed())
            throw new RuntimeException("Blocking is not allowed");
    }

}
