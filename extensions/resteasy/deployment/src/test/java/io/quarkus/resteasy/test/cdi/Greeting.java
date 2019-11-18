package io.quarkus.resteasy.test.cdi;

import io.quarkus.resteasy.test.cdi.internal.PublicHello;

/**
 *
 */
public class Greeting extends PublicHello {

    @Override
    public String greet() {
        return "Hello user";
    }
}
