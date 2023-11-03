package io.quarkus.security.webauthn.test;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Arc;
import io.quarkus.security.webauthn.WebAuthnSecurity;
import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.auth.webauthn.Authenticator;

/**
 * This UserProvider does not update or store credentials in the callback endpoint: you do it manually after calls to
 * {@link WebAuthnSecurity#login(io.quarkus.security.webauthn.WebAuthnLoginResponse, io.vertx.ext.web.RoutingContext)}
 * and {@link WebAuthnSecurity#register(io.quarkus.security.webauthn.WebAuthnRegisterResponse, io.vertx.ext.web.RoutingContext)}
 */
@ApplicationScoped
public class WebAuthnManualTestUserProvider extends WebAuthnTestUserProvider {

    @Override
    public Uni<List<Authenticator>> findWebAuthnCredentialsByCredID(String credId) {
        assertRequestContext();
        return super.findWebAuthnCredentialsByCredID(credId);
    }

    @Override
    public Uni<List<Authenticator>> findWebAuthnCredentialsByUserName(String userId) {
        assertRequestContext();
        return super.findWebAuthnCredentialsByUserName(userId);
    }

    @Override
    public Uni<Void> updateOrStoreWebAuthnCredentials(Authenticator authenticator) {
        assertRequestContext();
        return Uni.createFrom().nullItem();
    }

    private void assertRequestContext() {
        // allow this being used in the tests
        if (TestUtil.isTestThread())
            return;
        if (!Arc.container().requestContext().isActive()) {
            throw new AssertionError("Request context not active");
        }
    }
}
