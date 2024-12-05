package io.quarkus.security.webauthn.test;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.resteasy.reactive.server.core.BlockingOperationSupport;

import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;
import io.smallrye.mutiny.Uni;

/**
 * This UserProvider stores and updates the credentials in the callback endpoint, and checks that it's non-blocking
 */
@ApplicationScoped
public class WebAuthnNonBlockingTestUserProvider extends WebAuthnTestUserProvider {
    @Override
    public Uni<WebAuthnCredentialRecord> findByCredentialId(String credId) {
        assertBlockingNotAllowed();
        return super.findByCredentialId(credId);
    }

    @Override
    public Uni<List<WebAuthnCredentialRecord>> findByUserName(String userId) {
        assertBlockingNotAllowed();
        return super.findByUserName(userId);
    }

    @Override
    public Uni<Void> update(String credentialId, long counter) {
        assertBlockingNotAllowed();
        return super.update(credentialId, counter);
    }

    @Override
    public Uni<Void> store(WebAuthnCredentialRecord credentialRecord) {
        assertBlockingNotAllowed();
        return super.store(credentialRecord);
    }

    private void assertBlockingNotAllowed() {
        // allow this being used in the tests
        if (TestUtil.isTestThread())
            return;
        if (BlockingOperationSupport.isBlockingAllowed())
            throw new RuntimeException("Blocking should not be allowed");
    }
}
