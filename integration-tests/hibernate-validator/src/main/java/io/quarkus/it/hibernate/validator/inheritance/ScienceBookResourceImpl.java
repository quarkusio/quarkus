package io.quarkus.it.hibernate.validator.inheritance;

public class ScienceBookResourceImpl implements ScienceBookResource {

    @Override
    public String hello(String name) {
        return "Hello " + name;
    }
}
