package io.quarkus.oidc.test;

import java.security.Principal;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.jwt.Claims;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;

import io.quarkus.oidc.runtime.OidcJwtCallerPrincipal;
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
@Priority(1)
public class CustomIdentityProvider implements IdentityProvider<TokenAuthenticationRequest> {

    @Override
    public Class<TokenAuthenticationRequest> getRequestType() {
        return TokenAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(TokenAuthenticationRequest request, AuthenticationRequestContext context) {
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();

        TokenCredential credential = request.getToken();
        try {
            JwtClaims jwtClaims = new JwtConsumerBuilder()
                    .setSkipSignatureVerification()
                    .setSkipAllValidators()
                    .build().processToClaims(credential.getToken());
            jwtClaims.setClaim(Claims.raw_token.name(), credential.getToken());

            Principal principal = new OidcJwtCallerPrincipal(jwtClaims, credential);
            if ("jdoe".equals(principal.getName())) {
                throw new AuthenticationCompletionException();
            }
            builder.setPrincipal(principal);
        } catch (InvalidJwtException e) {
            throw new AuthenticationFailedException(e);
        }

        return Uni.createFrom().item(builder.build());

    }

}
