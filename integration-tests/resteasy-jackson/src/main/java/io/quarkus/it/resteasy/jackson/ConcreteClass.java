package io.quarkus.it.resteasy.jackson;

public class ConcreteClass extends AbstractResource {
    @Override
    public String message() {
        return "concrete";
    }
}
