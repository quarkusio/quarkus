package io.quarkus.funqy.test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.quarkus.funqy.Funq;

public class PrimitiveFunctions {
    @Funq
    public String toLowerCase(String val) {
        return val.toLowerCase();
    }

    @Funq
    public CompletionStage<String> toLowerCaseAsync(String val) {
        return CompletableFuture.completedFuture(val.toLowerCase());
    }

    @Funq
    public int doubleIt(int val) {
        return val * 2;
    }

    @Funq
    public CompletableFuture<Integer> doubleItAsync(int val) {
        return CompletableFuture.completedFuture(val * 2);
    }

}
