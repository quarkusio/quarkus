package io.quarkus.it.security.webauthn;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord.RequiredPersistedData;
import io.smallrye.mutiny.Uni;

@Entity
public class WebAuthnCredential extends PanacheEntityBase {

    /**
     * The non user identifiable id for the authenticator
     */
    @Id
    public String credID;

    /**
     * The public key associated with this authenticator
     */
    public byte[] publicKey;

    public long publicKeyAlgorithm;

    /**
     * The signature counter of the authenticator to prevent replay attacks
     */
    public long counter;

    public UUID aaguid;

    // owning side
    @OneToOne
    public User user;

    public WebAuthnCredential() {
    }

    public WebAuthnCredential(WebAuthnCredentialRecord credentialRecord, User user) {
        RequiredPersistedData requiredPersistedData = credentialRecord.getRequiredPersistedData();
        aaguid = requiredPersistedData.aaguid();
        counter = requiredPersistedData.counter();
        credID = requiredPersistedData.credentialId();
        publicKey = requiredPersistedData.publicKey();
        publicKeyAlgorithm = requiredPersistedData.publicKeyAlgorithm();
        this.user = user;
        user.webAuthnCredential = this;
    }

    public WebAuthnCredentialRecord toWebAuthnCredentialRecord() {
        return WebAuthnCredentialRecord
                .fromRequiredPersistedData(
                        new RequiredPersistedData(user.userName, credID, aaguid, publicKey, publicKeyAlgorithm, counter));
    }

    public static Uni<List<WebAuthnCredential>> findByUserName(String userName) {
        return list("user.userName", userName);
    }

    public static Uni<WebAuthnCredential> findByCredentialId(String credID) {
        return findById(credID);
    }

    public <T> Uni<T> fetch(T association) {
        return getSession().flatMap(session -> session.fetch(association));
    }
}
