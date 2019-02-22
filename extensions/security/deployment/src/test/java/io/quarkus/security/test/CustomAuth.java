package io.quarkus.security.test;

import static io.undertow.UndertowMessages.MESSAGES;
import static io.undertow.util.Headers.AUTHORIZATION;
import static io.undertow.util.Headers.BASIC;
import static io.undertow.util.Headers.WWW_AUTHENTICATE;
import static io.undertow.util.StatusCodes.UNAUTHORIZED;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import org.jboss.logging.Logger;

import io.undertow.UndertowLogger;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.FlexBase64;

/**
 * An alternate BASIC auth based mechanism to test installing a custom AuthenticationMechanism into Undertow
 */
public class CustomAuth implements AuthenticationMechanism {
    private static final Logger log = Logger.getLogger(CustomAuth.class.getName());
    private static final String BASIC_PREFIX = BASIC + " ";
    private static final String LOWERCASE_BASIC_PREFIX = BASIC_PREFIX.toLowerCase(Locale.ENGLISH);
    private static final int PREFIX_LENGTH = BASIC_PREFIX.length();
    private static final String COLON = ":";

    private IdentityManager identityManager;

    @Override
    public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext securityContext) {
        List<String> authHeaders = exchange.getRequestHeaders().get(AUTHORIZATION);
        log.info("CustomAuth, authHeaders: " + authHeaders);
        if (authHeaders != null) {
            for (String current : authHeaders) {
                if (current.toLowerCase(Locale.ENGLISH).startsWith(LOWERCASE_BASIC_PREFIX)) {

                    String base64Challenge = current.substring(PREFIX_LENGTH);
                    String plainChallenge = null;
                    try {
                        ByteBuffer decode = FlexBase64.decode(base64Challenge);

                        plainChallenge = new String(decode.array(), decode.arrayOffset(), decode.limit(),
                                StandardCharsets.UTF_8);
                        UndertowLogger.SECURITY_LOGGER.infof("Found basic auth header %s (decoded using charset %s) in %s",
                                plainChallenge, StandardCharsets.UTF_8, exchange);
                    } catch (IOException e) {
                        UndertowLogger.SECURITY_LOGGER.infof(e, "Failed to decode basic auth header %s in %s", base64Challenge,
                                exchange);
                    }
                    int colonPos;
                    if (plainChallenge != null && (colonPos = plainChallenge.indexOf(COLON)) > -1) {
                        String userName = plainChallenge.substring(0, colonPos);
                        char[] password = plainChallenge.substring(colonPos + 1).toCharArray();

                        IdentityManager idm = getIdentityManager(securityContext);
                        PasswordCredential credential = new PasswordCredential(password);
                        try {
                            final AuthenticationMechanismOutcome result;
                            Account account = idm.verify(userName, credential);
                            UndertowLogger.SECURITY_LOGGER.infof("Obtained account: %s", account);
                            if (account != null) {
                                UndertowLogger.SECURITY_LOGGER.infof("AUTHENTICATED, roles: %s", account.getRoles());
                                securityContext.authenticationComplete(account, "CUSTOM", false);
                                result = AuthenticationMechanismOutcome.AUTHENTICATED;
                            } else {
                                securityContext.authenticationFailed(MESSAGES.authenticationFailed(userName), "CUSTOM");
                                result = AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
                            }
                            return result;
                        } finally {
                            for (int i = 0; i < password.length; i++) {
                                password[i] = 0x00;
                            }
                        }
                    }

                    // By this point we had a header we should have been able to verify but for some reason
                    // it was not correctly structured.
                    return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
                }
            }
        }
        return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
    }

    @Override
    public ChallengeResult sendChallenge(HttpServerExchange exchange, SecurityContext securityContext) {
        exchange.getResponseHeaders().add(WWW_AUTHENTICATE, "BASIC realm=CUSTOM");
        UndertowLogger.SECURITY_LOGGER.infof("Sending basic auth challenge for %s", exchange);
        return new ChallengeResult(true, UNAUTHORIZED);
    }

    @SuppressWarnings("deprecation")
    private IdentityManager getIdentityManager(SecurityContext securityContext) {
        return identityManager != null ? identityManager : securityContext.getIdentityManager();
    }

}
