package io.quarkus.it.testsupport.commandmode;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultBean implements CdiBean {
    @Override
    public String myMethod() {
        return "default bean";
    }
}
