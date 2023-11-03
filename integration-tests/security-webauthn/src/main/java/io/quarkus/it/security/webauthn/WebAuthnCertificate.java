package io.quarkus.it.security.webauthn;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
public class WebAuthnCertificate extends PanacheEntity {

    @ManyToOne
    public WebAuthnCredential webAuthnCredential;

    /**
     * The list of X509 certificates encoded as base64url.
     */
    public String x5c;
}
