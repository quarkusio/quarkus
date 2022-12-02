package io.quarkus.test.security.webauthn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.quarkus.security.webauthn.WebAuthnUserProvider;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.auth.webauthn.Authenticator;

/**
 * UserProvider suitable for tests, which stores and updates credentials in a list,
 * so you can use it in your tests.
 *
 * @see WebAuthnStoringTestUserProvider
 * @see WebAuthnManualTestUserProvider
 */
public class WebAuthnTestUserProvider implements WebAuthnUserProvider {

    private List<Authenticator> auths = new ArrayList<>();

    @Override
    public Uni<List<Authenticator>> findWebAuthnCredentialsByUserName(String userId) {
        List<Authenticator> ret = new ArrayList<>();
        for (Authenticator authenticator : auths) {
            if (authenticator.getUserName().equals(userId)) {
                ret.add(authenticator);
            }
        }
        return Uni.createFrom().item(ret);
    }

    @Override
    public Uni<List<Authenticator>> findWebAuthnCredentialsByCredID(String credId) {
        List<Authenticator> ret = new ArrayList<>();
        for (Authenticator authenticator : auths) {
            if (authenticator.getCredID().equals(credId)) {
                ret.add(authenticator);
            }
        }
        return Uni.createFrom().item(ret);
    }

    @Override
    public Uni<Void> updateOrStoreWebAuthnCredentials(Authenticator authenticator) {
        Authenticator existing = find(authenticator.getUserName(), authenticator.getCredID());
        if (existing != null) {
            // update
            existing.setCounter(authenticator.getCounter());
        } else {
            // add
            store(authenticator);
        }
        return Uni.createFrom().nullItem();
    }

    private Authenticator find(String userName, String credID) {
        for (Authenticator auth : auths) {
            if (userName.equals(auth.getUserName())
                    && credID.equals(auth.getCredID())) {
                // update
                return auth;
            }
        }
        return null;
    }

    @Override
    public Set<String> getRoles(String userId) {
        return Collections.singleton("admin");
    }

    /**
     * Stores a new credential
     *
     * @param authenticator the new credential to store
     */
    public void store(Authenticator authenticator) {
        auths.add(authenticator);
    }

    /**
     * Updates an existing credential
     *
     * @param userName the user name
     * @param credID the credential ID
     * @param counter the new counter value
     */
    public void update(String userName, String credID, long counter) {
        Authenticator authenticator = find(userName, credID);
        authenticator.setCounter(counter);
    }
}