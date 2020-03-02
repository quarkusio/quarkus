package io.quarkus.smallrye.jwt.runtime.auth;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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

/**
 * Validates a bearer token according to the MP-JWT rules
 */
@ApplicationScoped
public class MpJwtValidator implements IdentityProvider<TokenAuthenticationRequest> {

    private static final Logger log = Logger.getLogger(MpJwtValidator.class);

    final JWTParser parser;
    final JwtRolesMapper jwtRolesMapper;

    @Inject
    public MpJwtValidator(JWTParser parser, JwtRolesMapper jwtRolesMapper) {
        this.parser = parser;
        this.jwtRolesMapper = jwtRolesMapper;
    }

    @Override
    public Class<TokenAuthenticationRequest> getRequestType() {
        return TokenAuthenticationRequest.class;
    }

    @Override
    public CompletionStage<SecurityIdentity> authenticate(TokenAuthenticationRequest request,
            AuthenticationRequestContext context) {
        try {
            JsonWebToken jwt = parser.parse(request.getToken().getToken());

            return CompletableFuture
                    .completedFuture(QuarkusSecurityIdentity.builder().setPrincipal(jwt)
                            .addRoles(jwtRolesMapper.mapRoles(jwt))
                            .addAttribute(SecurityIdentity.USER_ATTRIBUTE, jwt).build());

        } catch (ParseException e) {
            log.debug("Authentication failed", e);
            CompletableFuture<SecurityIdentity> cf = new CompletableFuture<>();
            cf.completeExceptionally(new AuthenticationFailedException(e));
            return cf;
        }
    }
}
