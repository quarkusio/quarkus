package io.quarkus.arc.test.supplement;

import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SomeAsyncBeanInExternalLibrary {
    public CompletableFuture<String> hello() {
        return CompletableFuture.completedFuture("Hello, world!");
    }
}
