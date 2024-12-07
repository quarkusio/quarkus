package io.quarkus.test.security.webauthn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.webauthn4j.util.Base64UrlUtil;

import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.security.webauthn.WebAuthnUserProvider;
import io.smallrye.mutiny.Uni;

/**
 * UserProvider suitable for tests, which fetches and updates credentials from a list,
 * so you can use it in your tests.
 */
public class WebAuthnTestUserProvider implements WebAuthnUserProvider {

    private List<WebAuthnCredentialRecord> auths = new ArrayList<>();

    @Override
    public Uni<List<WebAuthnCredentialRecord>> findByUserName(String userId) {
        List<WebAuthnCredentialRecord> ret = new ArrayList<>();
        for (WebAuthnCredentialRecord authenticator : auths) {
            if (authenticator.getUserName().equals(userId)) {
                ret.add(authenticator);
            }
        }
        return Uni.createFrom().item(ret);
    }

    @Override
    public Uni<WebAuthnCredentialRecord> findByCredentialId(String credId) {
        byte[] bytes = Base64UrlUtil.decode(credId);
        for (WebAuthnCredentialRecord authenticator : auths) {
            if (Arrays.equals(authenticator.getAttestedCredentialData().getCredentialId(), bytes)) {
                return Uni.createFrom().item(authenticator);
            }
        }
        return Uni.createFrom().failure(new RuntimeException("Credentials not found for credential ID " + credId));
    }

    @Override
    public Uni<Void> update(String credentialId, long counter) {
        reallyUpdate(credentialId, counter);
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<Void> store(WebAuthnCredentialRecord credentialRecord) {
        reallyStore(credentialRecord);
        return Uni.createFrom().voidItem();
    }

    @Override
    public Set<String> getRoles(String userId) {
        return Collections.singleton("admin");
    }

    // For tests

    public void clear() {
        auths.clear();
    }

    public void reallyUpdate(String credentialId, long counter) {
        byte[] bytes = Base64UrlUtil.decode(credentialId);
        for (WebAuthnCredentialRecord authenticator : auths) {
            if (Arrays.equals(authenticator.getAttestedCredentialData().getCredentialId(), bytes)) {
                authenticator.setCounter(counter);
                break;
            }
        }
    }

    public void reallyStore(WebAuthnCredentialRecord credentialRecord) {
        auths.add(credentialRecord);
    }
}