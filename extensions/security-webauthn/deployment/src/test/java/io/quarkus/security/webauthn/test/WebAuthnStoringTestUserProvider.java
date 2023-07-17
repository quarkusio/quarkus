package io.quarkus.security.webauthn.test;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;

/**
 * This UserProvider stores and updates the credentials in the callback endpoint
 */
@ApplicationScoped
public class WebAuthnStoringTestUserProvider extends WebAuthnTestUserProvider {
}
