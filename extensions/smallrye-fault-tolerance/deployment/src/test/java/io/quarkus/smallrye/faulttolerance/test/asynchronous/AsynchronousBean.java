package io.quarkus.smallrye.faulttolerance.test.asynchronous;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

@ApplicationScoped
public class AsynchronousBean {

    @Asynchronous
    public CompletionStage<String> asynchronous() {
        return CompletableFuture.completedFuture("hello");
    }
}
