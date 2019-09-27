package io.quarkus.elytron.security.oauth2.runtime.auth;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;

public class OAuth2Augmentor implements SecurityIdentityAugmentor {

    private final String roleClaim;

    public OAuth2Augmentor(String roleClaim) {
        this.roleClaim = roleClaim;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public CompletionStage<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        CompletableFuture<SecurityIdentity> cs = new CompletableFuture<>();
        if (identity.getPrincipal() instanceof ElytronOAuth2CallerPrincipal) {
            QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder()
                    .setPrincipal(identity.getPrincipal())
                    .addAttributes(identity.getAttributes())
                    .addCredentials(identity.getCredentials())
                    .addRoles(identity.getRoles());
            String[] roles = extractRoles(((ElytronOAuth2CallerPrincipal) identity.getPrincipal()));
            if (roles != null) {
                for (String i : roles) {
                    builder.addRole(i);
                }
            }
            cs.complete(builder.build());
        } else {
            cs.complete(identity);
        }
        return cs;
    }

    private String[] extractRoles(ElytronOAuth2CallerPrincipal principal) {
        Object claims = principal.getClaims().get(roleClaim);
        if (claims instanceof List) {
            return ((List<String>) claims).toArray(new String[0]);
        }

        String claim = (String) principal.getClaims().get(roleClaim);
        if (claim == null) {
            return null;
        }
        return claim.split(" ");
    }
}
