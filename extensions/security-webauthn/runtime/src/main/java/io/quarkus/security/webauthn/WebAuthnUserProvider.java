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
     * @param username the username
     * @return a list of credentials for this username, or an empty list if there are no credentials or if the user name is
     *         not found.
     */
    public Uni<List<WebAuthnCredentialRecord>> findByUsername(String username);

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
     * Update an existing WebAuthn credential's counter. This is only used by the default login endpoint, which
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
     * Store a new WebAuthn credential. This is only used by the default registration endpoint, which
     * is disabled by default and can be enabled via the <code>quarkus.webauthn.enable-registration-endpoint</code>.
     * You don't have to implement this method if you handle registration manually via
     * {@link WebAuthnSecurity#register(WebAuthnRegisterResponse, io.vertx.ext.web.RoutingContext)}
     *
     * Make sure that you never allow creating
     * new credentials for a `username` that already exists. Otherwise you risk allowing third-parties to impersonate existing
     * users by letting them add their own credentials to existing accounts. If you want to allow existing users to register
     * more than one WebAuthn credential, you must make sure that the user is currently logged
     * in under the same <code>username</code> to which you want to add new credentials. In every other case, make sure to
     * return a failed
     * {@link Uni} from this method.
     *
     * The default behaviour is to not do anything.
     *
     * @param credentialRecord the new credentials to store
     * @return a uni completion object
     * @throws Exception a failed {@link Uni} if the <code>credentialId</code> already exists, or the <code>username</code>
     *         already
     *         has a credential and you disallow having more, or if trying to add credentials to other users than the current
     *         user.
     */
    public default Uni<Void> store(WebAuthnCredentialRecord credentialRecord) {
        return Uni.createFrom().voidItem();
    }

    /**
     * Returns the set of roles for the given username
     *
     * @param username the username
     * @return the set of roles (defaults to an empty set)
     */
    public default Set<String> getRoles(String username) {
        return Collections.emptySet();
    }
}
