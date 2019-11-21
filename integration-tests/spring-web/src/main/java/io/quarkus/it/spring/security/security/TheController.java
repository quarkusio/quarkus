package io.quarkus.it.spring.security.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TheController {

    @Autowired
    @Qualifier("serviceWithSecuredMethods")
    ServiceWithSecuredMethods serviceWithSecuredMethods;

    @Autowired
    @Qualifier("subclassService")
    Subclass subclass;

    @Autowired
    @Qualifier("securedService")
    SecuredService securedService;

    @GetMapping("/restrictedOnClass")
    public String noAdditionalConstraints() {
        return securedService.noAdditionalConstraints();
    }

    @GetMapping("/restrictedOnMethod")
    public String restrictedOnMethod() {
        return securedService.restrictedOnMethod();
    }

    @GetMapping("/securedMethod")
    public String securedMethod() {
        return serviceWithSecuredMethods.securedMethod();
    }

    @GetMapping("/accessibleForAllMethod")
    public String accessibleMethod() {
        return subclass.accessibleForAll();
    }

}
