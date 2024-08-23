package io.quarkus.smallrye.faulttolerance.test.asynchronous;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class AsynchronousTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(AsynchronousBean.class));

    @Inject
    AsynchronousBean asynchronous;

    @Test
    public void testAsynchronous() throws ExecutionException, InterruptedException {
        assertEquals("hello", asynchronous.hello().toCompletableFuture().get());
    }
}
