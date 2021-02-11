package io.quarkus.elytron.security.oauth2.runtime.auth;

import java.util.List;
import java.util.function.Supplier;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

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
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        if (identity.getPrincipal() instanceof ElytronOAuth2CallerPrincipal) {

            return Uni.createFrom().item(new Supplier<SecurityIdentity>() {
                @Override
                public SecurityIdentity get() {
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
                    return builder.build();
                }
            });
        } else {
            return Uni.createFrom().item(identity);
        }
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
