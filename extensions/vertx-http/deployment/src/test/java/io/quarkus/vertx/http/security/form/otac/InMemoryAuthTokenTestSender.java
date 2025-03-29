package io.quarkus.vertx.http.security.form.otac;

import java.util.Arrays;

import jakarta.inject.Singleton;

import io.quarkus.security.credential.PasswordCredential;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.security.token.OneTimeAuthenticationTokenSender;
import io.smallrye.mutiny.Uni;

@Singleton
public final class InMemoryAuthTokenTestSender implements OneTimeAuthenticationTokenSender {

    private volatile char[] token;

    @Override
    public Uni<Void> send(SecurityIdentity securityIdentity, PasswordCredential oneTimeTokenCredential) {
        this.token = Arrays.copyOf(oneTimeTokenCredential.getPassword(), oneTimeTokenCredential.getPassword().length);
        return Uni.createFrom().voidItem();
    }

    char[] getToken() {
        return token;
    }

    void clean() {
        token = null;
    }
}
