package io.quarkus.it.amazon.lambda;

import io.quarkus.funqy.Funq;

public class FunqyGreeting {

    @Funq
    public String funqy(String name) {
        return "Make it funqy " + name;
    }
}
