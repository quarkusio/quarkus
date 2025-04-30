package io.quarkus.it.security.webauthn;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.security.webauthn.WebAuthnUserProvider;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MyWebAuthnSetup implements WebAuthnUserProvider {

    @WithTransaction
    @Override
    public Uni<List<WebAuthnCredentialRecord>> findByUsername(String username) {
        return WebAuthnCredential.findByUsername(username)
                .map(list -> list.stream().map(WebAuthnCredential::toWebAuthnCredentialRecord).toList());
    }

    @WithTransaction
    @Override
    public Uni<WebAuthnCredentialRecord> findByCredentialId(String credentialId) {
        return WebAuthnCredential.findByCredentialId(credentialId)
                .onItem().ifNull().failWith(() -> new RuntimeException("No such credentials"))
                .map(WebAuthnCredential::toWebAuthnCredentialRecord);
    }

    @WithTransaction
    @Override
    public Uni<Void> store(WebAuthnCredentialRecord credentialRecord) {
        // this user is handled in the LoginResource endpoint manually
        if (credentialRecord.getUsername().equals("scooby")) {
            return Uni.createFrom().voidItem();
        }
        User newUser = new User();
        newUser.username = credentialRecord.getUsername();
        WebAuthnCredential credential = new WebAuthnCredential(credentialRecord, newUser);
        return credential.persist()
                .flatMap(c -> newUser.persist())
                .onItem().ignore().andContinueWithNull();
    }

    @WithTransaction
    @Override
    public Uni<Void> update(String credentialId, long counter) {
        return WebAuthnCredential.findByCredentialId(credentialId)
                .invoke(credential -> {
                    // this user is handled in the LoginResource endpoint manually
                    if (!credential.user.username.equals("scooby")) {
                        credential.counter = counter;
                    }
                })
                .onItem().ignore().andContinueWithNull();
    }

    @Override
    public Set<String> getRoles(String username) {
        if (username.equals("admin")) {
            Set<String> ret = new HashSet<>();
            ret.add("user");
            ret.add("admin");
            return ret;
        }
        return Collections.singleton("user");
    }
}
