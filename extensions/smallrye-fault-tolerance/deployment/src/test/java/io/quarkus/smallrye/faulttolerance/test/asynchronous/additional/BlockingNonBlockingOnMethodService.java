package io.quarkus.smallrye.faulttolerance.test.asynchronous.additional;

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;

@ApplicationScoped
public class BlockingNonBlockingOnMethodService {
    @Retry
    @Blocking
    @NonBlocking
    public CompletionStage<String> hello() {
        throw new IllegalArgumentException();
    }
}
