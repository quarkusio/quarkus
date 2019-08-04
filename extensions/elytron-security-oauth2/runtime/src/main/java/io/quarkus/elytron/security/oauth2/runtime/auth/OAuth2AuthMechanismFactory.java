package io.quarkus.elytron.security.oauth2.runtime.auth;

import java.util.Map;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMechanismFactory;
import io.undertow.security.idm.IdentityManager;
import io.undertow.server.handlers.form.FormParserFactory;

/**
 * An AuthenticationMechanismFactory for OAuth2
 */
public class OAuth2AuthMechanismFactory implements AuthenticationMechanismFactory {

    /**
     * This builds the OAuth2AuthMechanism with a JWTAuthContextInfo containing the issuer and signer public key needed
     * to validate the token. This information is currently taken from the query parameters passed in via the
     * web.xml/login-config/auth-method value, or via CDI injection.
     *
     * @param mechanismName - the login-config/auth-method, which will be MP-JWT for OAuth2AuthMechanism
     * @param formParserFactory - unused form type of authentication factory
     * @param properties - the query parameters from the web.xml/login-config/auth-method value. We look for an issuedBy
     *        and signerPubKey property to use for token validation.
     * @return the OAuth2AuthMechanism
     *
     */
    @Override
    public AuthenticationMechanism create(String mechanismName, IdentityManager identityManager,
            FormParserFactory formParserFactory, final Map<String, String> properties) {
        return new OAuth2AuthMechanism(identityManager);
    }

}
