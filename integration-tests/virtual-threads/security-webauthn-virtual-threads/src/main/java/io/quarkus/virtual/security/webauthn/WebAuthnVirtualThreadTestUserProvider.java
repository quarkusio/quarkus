package io.quarkus.virtual.security.webauthn;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;
import io.quarkus.test.vertx.VirtualThreadsAssertions;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.auth.webauthn.Authenticator;

/**
 * This UserProvider stores and updates the credentials in the callback endpoint, but is blocking
 */
@ApplicationScoped
@RunOnVirtualThread
public class WebAuthnVirtualThreadTestUserProvider extends WebAuthnTestUserProvider {
    @Override
    public Uni<List<Authenticator>> findWebAuthnCredentialsByCredID(String credId) {
        assertVirtualThread();
        return super.findWebAuthnCredentialsByCredID(credId);
    }

    @Override
    public Uni<List<Authenticator>> findWebAuthnCredentialsByUserName(String userId) {
        assertVirtualThread();
        return super.findWebAuthnCredentialsByUserName(userId);
    }

    @Override
    public Uni<Void> updateOrStoreWebAuthnCredentials(Authenticator authenticator) {
        assertVirtualThread();
        return super.updateOrStoreWebAuthnCredentials(authenticator);
    }

    private void assertVirtualThread() {
        // allow this being used in the tests
        if (isTestThread())
            return;
        VirtualThreadsAssertions.assertEverything();
    }

    static boolean isTestThread() {
        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
            if (stackTraceElement.getClassName().equals("io.quarkus.test.junit.QuarkusTestExtension"))
                return true;
        }
        return false;
    }

}
