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

    public MpJwtValidator() {
        this.parser = null;
    }

    @Inject
    public MpJwtValidator(JWTParser parser) {
        this.parser = parser;
    }

    @Override
    public Class<TokenAuthenticationRequest> getRequestType() {
        return TokenAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(TokenAuthenticationRequest request,
            AuthenticationRequestContext context) {
        return Uni.createFrom().emitter(new Consumer<UniEmitter<? super SecurityIdentity>>() {
            @Override
            public void accept(UniEmitter<? super SecurityIdentity> uniEmitter) {
                try {
                    JsonWebToken jwtPrincipal = parser.parse(request.getToken().getToken());
                    uniEmitter.complete(QuarkusSecurityIdentity.builder().setPrincipal(jwtPrincipal)
                            .addRoles(jwtPrincipal.getGroups())
                            .addAttribute(SecurityIdentity.USER_ATTRIBUTE, jwtPrincipal).build());

                } catch (ParseException e) {
                    log.debug("Authentication failed", e);
                    uniEmitter.fail(new AuthenticationFailedException(e));
                }
            }
        });

    }
}
