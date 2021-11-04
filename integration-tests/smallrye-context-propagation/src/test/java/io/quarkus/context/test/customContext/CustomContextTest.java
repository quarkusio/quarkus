package io.quarkus.context.test.customContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that custom context can be declared and is propagated.
 * Note that default TC (and ME) propagate ALL contexts.
 */
public class CustomContextTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(CustomContextTest.class, CustomContext.class, CustomContextProvider.class)
                    .addAsServiceProvider(ThreadContextProvider.class, CustomContextProvider.class));

    @Inject
    ThreadContext tc;

    @Test
    public void testCustomContextPropagation() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // set something to custom context
        CustomContext.set("foo");

        CompletableFuture<String> ret = tc.withContextCapture(CompletableFuture.completedFuture("void"));
        CompletableFuture<Void> cfs = ret.thenApplyAsync(text -> {
            Assertions.assertEquals("foo", CustomContext.get());
            return null;
        }, executor);
        cfs.get();
    }
}
