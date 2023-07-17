package io.quarkus.security.test;

import static io.undertow.httpcore.HttpHeaderNames.BASIC;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.credential.PasswordCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * An alternate BASIC auth based mechanism to test installing a custom AuthenticationMechanism into Undertow
 */
@ApplicationScoped
public class CustomAuth implements HttpAuthenticationMechanism {
    private static final Logger log = Logger.getLogger(CustomAuth.class.getName());
    private static final String BASIC_PREFIX = BASIC + " ";
    private static final String LOWERCASE_BASIC_PREFIX = BASIC_PREFIX.toLowerCase(Locale.ENGLISH);
    private static final int PREFIX_LENGTH = BASIC_PREFIX.length();
    private static final String COLON = ":";
    protected static final ChallengeData CHALLENGE_DATA = new ChallengeData(
            HttpResponseStatus.UNAUTHORIZED.code(),
            HttpHeaderNames.WWW_AUTHENTICATE,
            "BASIC realm=CUSTOM");

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager identityProviderManager) {
        List<String> authHeaders = context.request().headers().getAll(HttpHeaderNames.AUTHORIZATION);
        if (authHeaders != null) {
            for (String current : authHeaders) {
                if (current.toLowerCase(Locale.ENGLISH).startsWith(LOWERCASE_BASIC_PREFIX)) {

                    String base64Challenge = current.substring(PREFIX_LENGTH);
                    String plainChallenge = null;
                    byte[] decode = Base64.getDecoder().decode(base64Challenge);

                    plainChallenge = new String(decode, StandardCharsets.UTF_8);
                    log.debugf("Found basic auth header %s (decoded using charset %s)", plainChallenge, StandardCharsets.UTF_8);
                    int colonPos;
                    if ((colonPos = plainChallenge.indexOf(COLON)) > -1) {
                        String userName = plainChallenge.substring(0, colonPos);
                        char[] password = plainChallenge.substring(colonPos + 1).toCharArray();

                        UsernamePasswordAuthenticationRequest credential = new UsernamePasswordAuthenticationRequest(userName,
                                new PasswordCredential(password));
                        return identityProviderManager.authenticate(credential);
                    }

                    // By this point we had a header we should have been able to verify but for some reason
                    // it was not correctly structured.
                    return Uni.createFrom().failure(new AuthenticationFailedException());
                }
            }
        }

        // No suitable header has been found in this request,
        return Uni.createFrom().nullItem();
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        return Uni.createFrom().item(CHALLENGE_DATA);
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return Collections.singleton(UsernamePasswordAuthenticationRequest.class);
    }

    @Override
    public HttpCredentialTransport getCredentialTransport() {
        return new HttpCredentialTransport(HttpCredentialTransport.Type.AUTHORIZATION, "basic");
    }
}
