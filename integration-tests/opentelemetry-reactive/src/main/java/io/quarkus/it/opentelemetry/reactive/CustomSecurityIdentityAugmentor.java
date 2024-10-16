package io.quarkus.it.opentelemetry.reactive;

import java.util.Map;

import jakarta.inject.Singleton;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.smallrye.mutiny.Uni;

@Singleton
public class CustomSecurityIdentityAugmentor implements SecurityIdentityAugmentor {
    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity securityIdentity,
            AuthenticationRequestContext authenticationRequestContext) {
        return augment(securityIdentity, authenticationRequestContext, Map.of());
    }

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context,
            Map<String, Object> attributes) {
        var routingContext = HttpSecurityUtils.getRoutingContextAttribute(attributes);
        if (routingContext != null) {
            var augmentorScenario = routingContext.normalizedPath().contains("-augmentor");
            var configRolesMappingScenario = routingContext.normalizedPath().contains("roles-mapping-http-perm");
            if (augmentorScenario || configRolesMappingScenario) {
                var builder = QuarkusSecurityIdentity.builder(identity);
                if (augmentorScenario) {
                    builder.addRole("AUGMENTOR");
                }
                if (configRolesMappingScenario) {
                    // this role is supposed to be re-mapped by HTTP roles mapping (not path-specific)
                    builder.addRole("ROLES-ALLOWED-MAPPING-ROLE");
                }
                return Uni.createFrom().item(builder.build());
            }
        }
        return Uni.createFrom().item(identity);
    }
}
