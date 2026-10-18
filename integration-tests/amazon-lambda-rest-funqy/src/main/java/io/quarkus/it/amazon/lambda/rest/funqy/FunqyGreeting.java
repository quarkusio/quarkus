package io.quarkus.it.amazon.lambda.rest.funqy;

import io.quarkus.funqy.Funq;
import io.smallrye.mutiny.Uni;

public class FunqyGreeting {

    @Funq
    public String funqy(String name) {
        return "Make it funqy " + name;
    }

    @Funq
    public Uni<String> funqyAsync(String name) {
        return Uni.createFrom().item(() -> "Make it funqy " + name);
    }
}
