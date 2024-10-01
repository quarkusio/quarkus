package io.quarkus.virtual.security.webauthn;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;
import io.quarkus.test.vertx.VirtualThreadsAssertions;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;

/**
 * This UserProvider stores and updates the credentials in the callback endpoint, but is blocking
 */
@ApplicationScoped
@RunOnVirtualThread
public class WebAuthnVirtualThreadTestUserProvider extends WebAuthnTestUserProvider {
    @Override
    public Uni<WebAuthnCredentialRecord> findByCredentialId(String credId) {
        assertVirtualThread();
        return super.findByCredentialId(credId);
    }

    @Override
    public Uni<List<WebAuthnCredentialRecord>> findByUserName(String userId) {
        assertVirtualThread();
        return super.findByUserName(userId);
    }

    @Override
    public Uni<Void> store(WebAuthnCredentialRecord credentialRecord) {
        assertVirtualThread();
        return super.store(credentialRecord);
    }

    @Override
    public Uni<Void> update(String credentialId, long counter) {
        assertVirtualThread();
        return super.update(credentialId, counter);
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
