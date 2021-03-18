package io.quarkus.resteasy.test.cdi;

import javax.enterprise.context.Dependent;

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
