package io.quarkus.it.spring.security;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service("serviceWithPreAuthorize")
public class ServiceWithPreAuthorize {

    @PreAuthorize("hasRole('user') or hasRole(@roles.VIEWER)")
    public String allowedForUserOrViewer() {
        return "allowedForUserOrViewer";
    }

    @PreAuthorize("@alwaysFalseChecker.check(#input)")
    public String withAlwaysFalseChecker(String input) {
        return "withAlwaysFalseChecker";
    }
}
