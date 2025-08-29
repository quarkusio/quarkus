package io.quarkus.arc.test.shutdown;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.ShutdownDelayInitiatedEvent;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.test.QuarkusUnitTest;

public class ShutdownDelayInitiatedEventTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(ShutdownMethods.class)
                    .addAsResource(new StringAsset("quarkus.shutdown.delay-enabled=true"),
                            "application.properties"))
            .setAllowTestClassOutsideDeployment(true)
            .setAfterUndeployListener(() -> {
                assertEquals(3, Messages.MESSAGES.size());
                assertEquals("shutdown_pc", Messages.MESSAGES.get(0));
                assertEquals("pre_shutdown", Messages.MESSAGES.get(1));
                assertEquals("shutdown", Messages.MESSAGES.get(2));
            });

    @Test
    public void test() {
    }

    @BeforeEach
    public void clearMessages() {
        Messages.MESSAGES.clear();
    }

    @ApplicationScoped
    static class ShutdownMethods {

        public void preShutdown(@Observes ShutdownDelayInitiatedEvent event) {
            Messages.MESSAGES.add("pre_shutdown");
        }

        public void preShutdown(@Observes ShutdownEvent event) {
            Messages.MESSAGES.add("shutdown");
        }

        @PostConstruct
        void init() {
            Messages.MESSAGES.add("shutdown_pc");
        }

    }
}
