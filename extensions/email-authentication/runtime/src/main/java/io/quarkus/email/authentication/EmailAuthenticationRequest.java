package io.quarkus.email.authentication;

import java.util.Objects;

import io.quarkus.security.identity.request.BaseAuthenticationRequest;

/**
 * Request to authenticate based on the email address.
 */
public final class EmailAuthenticationRequest extends BaseAuthenticationRequest {

    private final String emailAddress;

    public EmailAuthenticationRequest(String emailAddress) {
        this.emailAddress = Objects.requireNonNull(emailAddress);
    }

    public String getEmailAddress() {
        return emailAddress;
    }
}
