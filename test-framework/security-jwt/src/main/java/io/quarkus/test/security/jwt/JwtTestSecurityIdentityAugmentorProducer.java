package io.quarkus.test.security.jwt;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.arc.Unremovable;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.test.security.TestSecurityIdentityAugmentor;

@ApplicationScoped
public class JwtTestSecurityIdentityAugmentorProducer {

    @Produces
    @Unremovable
    public TestSecurityIdentityAugmentor produce() {
        return new JwtTestSecurityIdentityAugmentor();
    }

    private static class JwtTestSecurityIdentityAugmentor implements TestSecurityIdentityAugmentor {

        @Override
        public SecurityIdentity augment(final SecurityIdentity identity, final Annotation[] annotations) {
            QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);

            final JwtSecurity jwtSecurity = findJwtSecurity(annotations);
            builder.setPrincipal(new JsonWebToken() {

                @Override
                public String getName() {
                    return identity.getPrincipal().getName();
                }

                @SuppressWarnings("unchecked")
                @Override
                public <T> T getClaim(String claimName) {
                    if (Claims.groups.name().equals(claimName)) {
                        return (T) identity.getRoles();
                    }
                    if (jwtSecurity != null && jwtSecurity.claims() != null) {
                        for (Claim claim : jwtSecurity.claims()) {
                            if (claim.key().equals(claimName)) {
                                return (T) claim.value();
                            }
                        }
                    }
                    return null;
                }

                @Override
                public Set<String> getClaimNames() {
                    return null;
                }

            });

            return builder.build();
        }

        private JwtSecurity findJwtSecurity(Annotation[] annotations) {
            for (Annotation ann : annotations) {
                if (ann instanceof JwtSecurity) {
                    return (JwtSecurity) ann;
                }
            }
            return null;
        }
    }
}
