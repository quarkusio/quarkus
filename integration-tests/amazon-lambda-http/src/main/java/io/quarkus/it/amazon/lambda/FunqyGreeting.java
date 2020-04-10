package io.quarkus.it.amazon.lambda;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.quarkus.funqy.Funq;

public class FunqyGreeting {

    @Funq
    public String funqy(String name) {
        return "Make it funqy " + name;
    }

    @Funq
    public CompletionStage<String> funqyAsync(String name) {
        return CompletableFuture.completedFuture("Make it funqy " + name);
    }
}
