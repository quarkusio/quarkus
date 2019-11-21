package io.quarkus.spring.security.deployment.springapp;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class SpringService {

    @PreAuthorize("hasAnyRole(@roles.USER, @roles.ADMIN)")
    public String accessibleForUserAndAdmin() {
        return "accessibleForUserAndAdmin";
    }

    @PreAuthorize("hasAnyRole(@roles.USER, 'admin')")
    public String accessibleForUserAndAdminMixedTypes() {
        return "accessibleForUserAndAdminMixedTypes";
    }

    @PreAuthorize("isAuthenticated()")
    public String accessibleByAuthenticatedUsers() {
        return "authenticated";
    }

    @PreAuthorize("isAnonymous()")
    public String accessibleByAnonymousUser() {
        return "anonymous";
    }
}
