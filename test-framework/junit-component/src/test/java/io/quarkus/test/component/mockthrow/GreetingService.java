package io.quarkus.test.component.mockthrow;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GreetingService {

    @Inject
    GreetingSubService greetingSubService;

    public String greeting() {
        return greetingSubService.greet("hello there!");
    }

}
