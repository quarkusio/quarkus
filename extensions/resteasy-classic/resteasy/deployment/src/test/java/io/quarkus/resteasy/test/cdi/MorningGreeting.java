package io.quarkus.resteasy.test.cdi;

import jakarta.enterprise.context.Dependent;

/**
 *
 */
@Dependent
public class MorningGreeting extends Greeting {

    @Override
    public String greet() {
        return "Good Morning";
    }
}
