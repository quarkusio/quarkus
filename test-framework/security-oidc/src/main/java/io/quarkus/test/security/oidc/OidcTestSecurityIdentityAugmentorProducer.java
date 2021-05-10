package io.quarkus.test.security.oidc;

import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jose4j.jwt.JwtClaims;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.runtime.OidcJwtCallerPrincipal;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.test.security.common.TestSecurityIdentityAugmentor;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.util.KeyUtils;

@ApplicationScoped
public class OidcTestSecurityIdentityAugmentorProducer {

    @Produces
    @Unremovable
    public TestSecurityIdentityAugmentor produce() {
        return new OidcTestSecurityIdentityAugmentor();
    }

    private static class OidcTestSecurityIdentityAugmentor implements TestSecurityIdentityAugmentor {

        @Override
        public SecurityIdentity augment(final SecurityIdentity identity) {
            QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);

            JwtClaims claims = new JwtClaims();
            claims.setClaim(Claims.preferred_username.name(), identity.getPrincipal().getName());
            claims.setClaim(Claims.groups.name(), identity.getRoles().stream().collect(Collectors.toList()));
            for (Map.Entry<String, Object> entry : identity.getAttributes().entrySet()) {
                if (entry.getKey().startsWith("claim.")) {
                    claims.setClaim(entry.getKey().substring("claim.".length()), entry.getValue());
                }
            }
            String jwt = generateToken(claims);
            IdTokenCredential idToken = new IdTokenCredential(jwt, null);
            AccessTokenCredential accessToken = new AccessTokenCredential(jwt, null);

            JsonWebToken principal = new OidcJwtCallerPrincipal(claims, idToken);
            builder.setPrincipal(principal);
            builder.addCredential(idToken);
            builder.addCredential(accessToken);

            return builder.build();
        }

        private String generateToken(JwtClaims claims) {
            try {
                return Jwt.claims(claims.getClaimsMap()).sign(KeyUtils.generateKeyPair(2048).getPrivate());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

}
