package io.quarkus.arc.test.startup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.Startup;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class StartupNonBlockingAnnotationTest {

    static final List<String> LOG = new CopyOnWriteArrayList<String>();

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(StartupMethods.class))
            .assertException(x -> {
                // Make sure we get the build-time error, not the startup one
                Assertions.assertTrue(x instanceof IllegalStateException);
                Assertions.assertEquals(
                        "Failed to find any non-blocking provider for startup actions. Either import the quarkus-vertx module, or do not declare non-blocking startup actions.",
                        x.getMessage());
            });

    @Test
    public void testStartup() {
        Assertions.fail("Should not start");
    }

    static class StartupMethods {

        @Startup
        Uni<Void> nonBlocking() {
            Assertions.fail("Should not be called");
            return Uni.createFrom().voidItem();
        }
    }
}
