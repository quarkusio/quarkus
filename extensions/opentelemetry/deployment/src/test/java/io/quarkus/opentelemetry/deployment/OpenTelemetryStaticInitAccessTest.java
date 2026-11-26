package io.quarkus.opentelemetry.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The default {@link OpenTelemetry} bean is created at runtime init: requesting it during static init fails, so that no
 * value read at build time ends up in a native image.
 */
public class OpenTelemetryStaticInitAccessTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(StaticInitObserver.class))
            .assertException(t -> assertThat(t).rootCause()
                    .hasMessageContaining(OpenTelemetry.class.getName())
                    .hasMessageContaining("must not be accessed during STATIC_INIT"));

    @Test
    void test() {
        Assertions.fail("Startup should have failed");
    }

    @ApplicationScoped
    public static class StaticInitObserver {
        void onStaticInit(@Observes @Initialized(ApplicationScoped.class) Object event, OpenTelemetry openTelemetry) {
        }
    }
}
