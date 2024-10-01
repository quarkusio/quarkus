package io.quarkus.security.webauthn;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.smallrye.mutiny.Uni;

/**
 * Implement this interface in order to tell Quarkus WebAuthn how to look up
 * WebAuthn credentials, store new credentials, or update the credentials' counter,
 * as well as what roles those credentials map to.
 */
public interface WebAuthnUserProvider {
    /**
     * Look up a WebAuthn credential by username. This should return an empty list Uni if the user name is not found.
     *
     * @param userName the username
     * @return a list of credentials for this username, or an empty list if there are no credentials or if the user name is
     *         not found.
     */
    public Uni<List<WebAuthnCredentialRecord>> findByUserName(String userName);

    /**
     * Look up a WebAuthn credential by credential ID, this should return an exception Uni rather than return a null-item Uni
     * in case the credential is not found.
     *
     * @param credentialId the credential ID
     * @return a credentials for this credential ID.
     * @throws an exception Uni if the credential ID is unknown
     */
    public Uni<WebAuthnCredentialRecord> findByCredentialId(String credentialId);

    /**
     * Update an existing WebAuthn credential's counter. This is only used by the default login enpdoint, which
     * is disabled by default and can be enabled via the <code>quarkus.webauthn.enable-login-endpoint</code>.
     * You don't have to implement this method
     * if you handle logins manually via {@link WebAuthnSecurity#login(WebAuthnLoginResponse, io.vertx.ext.web.RoutingContext)}.
     *
     * The default behaviour is to not do anything.
     *
     * @param credentialId the credential ID
     * @return a uni completion object
     */
    public default Uni<Void> update(String credentialId, long counter) {
        return Uni.createFrom().voidItem();
    }

    /**
     * Store a new WebAuthn credential. This is only used by the default registration enpdoint, which
     * is disabled by default and can be enabled via the <code>quarkus.webauthn.enable-registration-endpoint</code>.
     * You don't have to implement this method if you handle registration manually via
     * {@link WebAuthnSecurity#register(WebAuthnRegisterResponse, io.vertx.ext.web.RoutingContext)}
     *
     * The default behaviour is to not do anything.
     *
     * @param userName the userName's credentials
     * @param credentialRecord the new credentials to store
     * @return a uni completion object
     */
    public default Uni<Void> store(WebAuthnCredentialRecord credentialRecord) {
        return Uni.createFrom().voidItem();
    }

    /**
     * Returns the set of roles for the given username
     *
     * @param userName the username
     * @return the set of roles (defaults to an empty set)
     */
    public default Set<String> getRoles(String userName) {
        return Collections.emptySet();
    }
}
