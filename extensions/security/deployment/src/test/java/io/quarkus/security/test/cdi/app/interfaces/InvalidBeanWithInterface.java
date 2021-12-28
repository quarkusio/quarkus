package io.quarkus.security.test.cdi.app.interfaces;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InvalidBeanWithInterface implements InvalidInterface1, InvalidInterface2 {

    @Override
    public String securedMethod() {
        return "some value";
    }

}
