package io.quarkus.smallrye.jwt.runtime.auth;

import java.util.Map;

import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMechanismFactory;
import io.undertow.security.idm.IdentityManager;
import io.undertow.server.handlers.form.FormParserFactory;

/**
 * An AuthenticationMechanismFactory for the MicroProfile JWT RBAC
 */
public class JWTAuthMechanismFactory implements AuthenticationMechanismFactory {

    public JWTAuthMechanismFactory() {
    }

    /**
     * This builds the JWTAuthMechanism with a JWTAuthContextInfo containing the issuer and signer public key needed
     * to validate the token. This information is currently taken from the query parameters passed in via the
     * web.xml/login-config/auth-method value, or via CDI injection.
     *
     * @param mechanismName - the login-config/auth-method, which will be MP-JWT for JWTAuthMechanism
     * @param formParserFactory - unused form type of authentication factory
     * @param properties - the query parameters from the web.xml/login-config/auth-method value. We look for an issuedBy
     *        and signerPubKey property to use for token validation.
     * @return the JWTAuthMechanism
     * @see JWTAuthContextInfo
     *
     */
    @Override
    public AuthenticationMechanism create(String mechanismName, IdentityManager identityManager,
            FormParserFactory formParserFactory, final Map<String, String> properties) {
        return new JWTAuthMechanism(identityManager);
    }

}
