package io.quarkus.arc.test.shutdown;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.annotation.PostConstruct;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.Shutdown;
import io.quarkus.test.QuarkusUnitTest;

public class ShutdownAnnotationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(ShutdownMethods.class))
            .setAllowTestClassOutsideDeployment(true)
            .setAfterUndeployListener(() -> {
                assertEquals(3, Messages.MESSAGES.size());
                assertEquals("shutdown_pc", Messages.MESSAGES.get(0));
                assertEquals("shutdown_first", Messages.MESSAGES.get(1));
                assertEquals("shutdown_second", Messages.MESSAGES.get(2));
            });

    @Test
    public void test() {
    }

    // @ApplicationScoped is added automatically
    static class ShutdownMethods {

        @Shutdown
        String first() {
            Messages.MESSAGES.add("shutdown_first");
            return "ok";
        }

        @Shutdown(Integer.MAX_VALUE)
        void second() {
            Messages.MESSAGES.add("shutdown_second");
        }

        @PostConstruct
        void init() {
            Messages.MESSAGES.add("shutdown_pc");
        }

    }

}
