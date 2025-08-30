package io.quarkus.arc.test.shutdown;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.annotation.PostConstruct;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.Shutdown;
import io.quarkus.runtime.ShutdownDelayInitiated;
import io.quarkus.test.QuarkusUnitTest;

public class ShutdownAnnotationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(ShutdownMethods.class, ShutdownDelayInitializedMethods.class)
                    .addAsResource(new StringAsset("quarkus.shutdown.delay-enabled=true"),
                            "application.properties"))
            .setAllowTestClassOutsideDeployment(true)
            .setAfterUndeployListener(() -> {
                assertEquals(6, Messages.MESSAGES.size());
                assertEquals("shutdown_delay_pc", Messages.MESSAGES.get(0));
                assertEquals("shutdown_delay_first", Messages.MESSAGES.get(1));
                assertEquals("shutdown_delay_second", Messages.MESSAGES.get(2));
                assertEquals("shutdown_pc", Messages.MESSAGES.get(3));
                assertEquals("shutdown_first", Messages.MESSAGES.get(4));
                assertEquals("shutdown_second", Messages.MESSAGES.get(5));
            });

    @BeforeEach
    public void clearMessages() {
        Messages.MESSAGES.clear();
    }

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

    // @ApplicationScoped is added automatically
    static class ShutdownDelayInitializedMethods {

        @ShutdownDelayInitiated
        String first() {
            Messages.MESSAGES.add("shutdown_delay_first");
            return "ok";
        }

        @ShutdownDelayInitiated(Integer.MAX_VALUE)
        void second() {
            Messages.MESSAGES.add("shutdown_delay_second");
        }

        @PostConstruct
        void init() {
            Messages.MESSAGES.add("shutdown_delay_pc");
        }

    }

}
