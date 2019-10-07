package io.quarkus.security.test;

import static io.undertow.httpcore.HttpHeaderNames.BASIC;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.credential.PasswordCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.HTTPAuthenticationMechanism;
import io.undertow.security.idm.IdentityManager;
import io.vertx.ext.web.RoutingContext;

/**
 * An alternate BASIC auth based mechanism to test installing a custom AuthenticationMechanism into Undertow
 */
@ApplicationScoped
public class CustomAuth implements HTTPAuthenticationMechanism {
    private static final Logger log = Logger.getLogger(CustomAuth.class.getName());
    private static final String BASIC_PREFIX = BASIC + " ";
    private static final String LOWERCASE_BASIC_PREFIX = BASIC_PREFIX.toLowerCase(Locale.ENGLISH);
    private static final int PREFIX_LENGTH = BASIC_PREFIX.length();
    private static final String COLON = ":";

    private IdentityManager identityManager;

    @Override
    public CompletionStage<SecurityIdentity> authenticate(RoutingContext context,
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
                    CompletableFuture<SecurityIdentity> cf = new CompletableFuture<>();
                    cf.completeExceptionally(new AuthenticationFailedException());
                    return cf;
                }
            }
        }

        // No suitable header has been found in this request,
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Boolean> sendChallenge(RoutingContext context) {
        context.response().headers().set(HttpHeaderNames.WWW_AUTHENTICATE, "BASIC realm=CUSTOM");
        context.response().setStatusCode(HttpResponseStatus.UNAUTHORIZED.code());
        return CompletableFuture.completedFuture(true);
    }
}
