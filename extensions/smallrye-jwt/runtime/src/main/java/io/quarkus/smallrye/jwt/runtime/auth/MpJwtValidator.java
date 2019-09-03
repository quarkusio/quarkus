package io.quarkus.smallrye.jwt.runtime.auth;

import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.JwtContext;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.jwt.auth.principal.DefaultJWTTokenParser;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.ParseException;

/**
 * Validates a bearer token according to the MP-JWT rules
 */
@ApplicationScoped
public class MpJwtValidator implements IdentityProvider<TokenAuthenticationRequest> {

    private static final Logger log = Logger.getLogger(MpJwtValidator.class);

    final JWTAuthContextInfo authContextInfo;

    private DefaultJWTTokenParser parser = new DefaultJWTTokenParser();

    public MpJwtValidator() {
        authContextInfo = null;
    }

    @Inject
    public MpJwtValidator(JWTAuthContextInfo authContextInfo) {
        this.authContextInfo = authContextInfo;
    }

    @Override
    public Class<TokenAuthenticationRequest> getRequestType() {
        return TokenAuthenticationRequest.class;
    }

    @Override
    public CompletionStage<SecurityIdentity> authenticate(TokenAuthenticationRequest request,
            AuthenticationRequestContext context) {
        try {
            JwtContext jwtContext = parser.parse(request.getToken().getToken(), authContextInfo);

            JwtClaims claims = jwtContext.getJwtClaims();
            String name = claims.getClaimValue("upn", String.class);
            if (name == null) {
                name = claims.getClaimValue("preferred_username", String.class);
                if (name == null) {
                    name = claims.getSubject();
                }
            }
            QuarkusJwtCallerPrincipal principal = new QuarkusJwtCallerPrincipal(name, claims);
            return CompletableFuture
                    .completedFuture(QuarkusSecurityIdentity.builder().setPrincipal(principal)
                            .addRoles(new HashSet<>(claims.getStringListClaimValue("groups")))
                            .addAttribute(QuarkusSecurityIdentity.USER_ATTRIBUTE, principal).build());

        } catch (ParseException | MalformedClaimException e) {
            log.debug("Authentication failed", e);
            throw new AuthenticationFailedException();
        }
    }
}
