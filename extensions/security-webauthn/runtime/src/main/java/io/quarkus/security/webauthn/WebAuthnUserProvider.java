package io.quarkus.security.webauthn;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.smallrye.mutiny.Uni;
import io.vertx.ext.auth.webauthn.Authenticator;

/**
 * Implement this interface in order to tell Quarkus WebAuthn how to look up
 * WebAuthn credentials, store new credentials, or update the credentials' counter,
 * as well as what roles those credentials map to.
 */
public interface WebAuthnUserProvider {
    /**
     * Look up a WebAuthn credential by user name
     *
     * @param userName the user name
     * @return a list of credentials for this user name
     */
    public Uni<List<Authenticator>> findWebAuthnCredentialsByUserName(String userName);

    /**
     * Look up a WebAuthn credential by credential ID
     *
     * @param credentialId the credential ID
     * @returna list of credentials for this credential ID.
     */
    public Uni<List<Authenticator>> findWebAuthnCredentialsByCredID(String credentialId);

    /**
     * If this credential's combination of user and credential ID does not exist,
     * then store the new credential. If it already exists, then only update its counter
     *
     * @param authenticator the new credential if it does not exist, or the credential to update
     * @return a uni completion object
     */
    public Uni<Void> updateOrStoreWebAuthnCredentials(Authenticator authenticator);

    /**
     * Returns the set of roles for the given user name
     *
     * @param userName the user name
     * @return the set of roles (defaults to an empty set)
     */
    public default Set<String> getRoles(String userName) {
        return Collections.emptySet();
    }
}
