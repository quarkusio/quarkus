package io.quarkus.test.component.mockthrow;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreetingSubService {

    public String greet(String greeting) {
        return greeting;
    }

}
