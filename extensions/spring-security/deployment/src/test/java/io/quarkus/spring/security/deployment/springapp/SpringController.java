package io.quarkus.spring.security.deployment.springapp;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/secure")
public class SpringController {

    private final SpringComponent springComponent;

    public SpringController(SpringComponent springComponent) {
        this.springComponent = springComponent;
    }

    @GetMapping("/admin")
    public String accessibleForAdminOnly() {
        return springComponent.accessibleForAdminOnly();
    }
}
