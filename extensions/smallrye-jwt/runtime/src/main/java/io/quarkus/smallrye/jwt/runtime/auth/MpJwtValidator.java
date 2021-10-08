package io.quarkus.smallrye.jwt.runtime.auth;

import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;

/**
 * Validates a bearer token according to the MP-JWT rules
 */
@ApplicationScoped
public class MpJwtValidator implements IdentityProvider<TokenAuthenticationRequest> {

    private static final Logger log = Logger.getLogger(MpJwtValidator.class);

    final JWTParser parser;
    final boolean blockingAuthentication;

    public MpJwtValidator() {
        this.parser = null;
        this.blockingAuthentication = false;
    }

    @Inject
    public MpJwtValidator(JWTParser parser, SmallRyeJwtConfig config) {
        this.parser = parser;
        this.blockingAuthentication = config == null ? false : config.blockingAuthentication;
    }

    @Override
    public Class<TokenAuthenticationRequest> getRequestType() {
        return TokenAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(TokenAuthenticationRequest request,
            AuthenticationRequestContext context) {
        if (!blockingAuthentication) {
            return Uni.createFrom().emitter(new Consumer<UniEmitter<? super SecurityIdentity>>() {
                @Override
                public void accept(UniEmitter<? super SecurityIdentity> uniEmitter) {
                    try {
                        uniEmitter.complete(createSecurityIdentity(request));
                    } catch (AuthenticationFailedException e) {
                        uniEmitter.fail(e);
                    }
                }
            });
        } else {
            return context.runBlocking(() -> createSecurityIdentity(request));
        }

    }

    private SecurityIdentity createSecurityIdentity(TokenAuthenticationRequest request) {
        try {
            JsonWebToken jwtPrincipal = parser.parse(request.getToken().getToken());
            return QuarkusSecurityIdentity.builder().setPrincipal(jwtPrincipal)
                    .addCredential(request.getToken())
                    .addRoles(jwtPrincipal.getGroups())
                    .addAttribute(SecurityIdentity.USER_ATTRIBUTE, jwtPrincipal).build();
        } catch (ParseException e) {
            log.debug("Authentication failed", e);
            throw new AuthenticationFailedException(e);
        }
    }
}
