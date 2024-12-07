package io.quarkus.security.webauthn.test;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.resteasy.reactive.server.core.BlockingOperationSupport;

import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;

/**
 * This UserProvider stores and updates the credentials in the callback endpoint, but is blocking
 */
@ApplicationScoped
@Blocking
public class WebAuthnBlockingTestUserProvider extends WebAuthnTestUserProvider {
    @Override
    public Uni<WebAuthnCredentialRecord> findByCredentialId(String credId) {
        assertBlockingAllowed();
        return super.findByCredentialId(credId);
    }

    @Override
    public Uni<List<WebAuthnCredentialRecord>> findByUserName(String userId) {
        assertBlockingAllowed();
        return super.findByUserName(userId);
    }

    @Override
    public Uni<Void> update(String credentialId, long counter) {
        assertBlockingAllowed();
        return super.update(credentialId, counter);
    }

    @Override
    public Uni<Void> store(WebAuthnCredentialRecord credentialRecord) {
        assertBlockingAllowed();
        return super.store(credentialRecord);
    }

    private void assertBlockingAllowed() {
        if (!BlockingOperationSupport.isBlockingAllowed())
            throw new RuntimeException("Blocking is not allowed");
    }

}
