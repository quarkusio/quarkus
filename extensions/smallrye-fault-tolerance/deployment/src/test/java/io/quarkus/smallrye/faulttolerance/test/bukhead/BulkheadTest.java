package io.quarkus.smallrye.faulttolerance.test.bukhead;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class BulkheadTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(BulkheadBean.class));

    @Inject
    BulkheadBean bulkhead;

    @Test
    public void test() throws InterruptedException {
        AtomicBoolean success = new AtomicBoolean(true);
        AtomicBoolean bulkheadFailures = new AtomicBoolean(false);
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 20; i++) {
            executorService.submit(() -> {
                try {
                    int count = bulkhead.hello();
                    if (count > 5) {
                        success.set(false);
                    }
                } catch (BulkheadException be) {
                    bulkheadFailures.set(true);
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);
        assertTrue(success.get());
        assertTrue(bulkheadFailures.get());
    }
}
