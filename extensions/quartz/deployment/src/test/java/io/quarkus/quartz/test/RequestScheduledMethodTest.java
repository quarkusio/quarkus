package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class RequestScheduledMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(RequestContextJobs.class));

    @Test
    public void testRequestContextScheduledJobs() throws InterruptedException {
        assertTrue(RequestContextJobs.LATCH.await(5, TimeUnit.SECONDS));
    }
}
