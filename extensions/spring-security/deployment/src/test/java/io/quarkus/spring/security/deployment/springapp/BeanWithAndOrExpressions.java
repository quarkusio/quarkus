package io.quarkus.spring.security.deployment.springapp;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class BeanWithAndOrExpressions {

    @PreAuthorize("hasAnyRole('user', 'admin') and #user == principal.username")
    public String allowedForUser(String user) {
        return "allowedForUser";
    }

    @PreAuthorize("hasRole('user') OR hasRole('admin')")
    public String allowedForUserOrAdmin() {
        return "allowedForUserOrAdmin";
    }

    @PreAuthorize("hasAnyRole('superadmin1', 'superadmin2') OR isAnonymous() OR hasRole('admin')")
    public String allowedForAdminOrAnonymous() {
        return "allowedForAdminOrAnonymous";
    }
}
