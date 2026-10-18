package io.quarkus.it.virtual;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.funqy.Funq;

@ApplicationScoped
public class FunqyGreeting {

    @Funq
    public String funqy(String name) {
        return "Make it funqy " + name;
    }
}
