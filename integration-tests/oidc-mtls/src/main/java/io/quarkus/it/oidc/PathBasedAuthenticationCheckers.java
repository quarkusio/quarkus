package io.quarkus.it.oidc;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;

@ApplicationScoped
public class PathBasedAuthenticationCheckers {

    @PermissionChecker("mtls-jwt")
    boolean isMtlsAndBearerAuthentication(SecurityIdentity identity) {
        Map<String, SecurityIdentity> identities = HttpSecurityUtils.getSecurityIdentities(identity);
        if (identities != null) {
            return identities.containsKey("bearer") && identities.containsKey("x509");
        }
        return false;
    }

    @PermissionChecker("mtls-basic")
    boolean isMtlsAndBasicAuthentication(SecurityIdentity identity) {
        Map<String, SecurityIdentity> identities = HttpSecurityUtils.getSecurityIdentities(identity);
        if (identities != null) {
            return identities.containsKey("basic") && identities.containsKey("x509");
        }
        return false;
    }

}
