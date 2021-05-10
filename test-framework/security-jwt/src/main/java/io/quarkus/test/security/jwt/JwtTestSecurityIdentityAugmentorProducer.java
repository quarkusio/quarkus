package io.quarkus.test.security.jwt;

import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.arc.Unremovable;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.test.security.common.TestSecurityIdentityAugmentor;

@ApplicationScoped
public class JwtTestSecurityIdentityAugmentorProducer {

    @Produces
    @Unremovable
    public TestSecurityIdentityAugmentor produce() {
        return new JwtTestSecurityIdentityAugmentor();
    }

    private static class JwtTestSecurityIdentityAugmentor implements TestSecurityIdentityAugmentor {

        @Override
        public SecurityIdentity augment(final SecurityIdentity identity) {
            QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);

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
                    for (Map.Entry<String, Object> entry : identity.getAttributes().entrySet()) {
                        if (entry.getKey().startsWith("claim." + claimName)) {
                            return (T) entry.getValue();
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
    }
}
