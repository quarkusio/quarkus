package io.quarkus.smallrye.faulttolerance.test.asynchronous.additional;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;

@ApplicationScoped
public class BothAsyncOnMethodService {
    @Retry
    @Asynchronous
    @AsynchronousNonBlocking
    public CompletionStage<String> hello() {
        throw new IllegalArgumentException();
    }
}
