package io.quarkus.resteasy.test.cdi.internal;

/**
 *
 */
public class PublicHello extends Hello {

    public String greet() {
        return "Hello";
    }
}
