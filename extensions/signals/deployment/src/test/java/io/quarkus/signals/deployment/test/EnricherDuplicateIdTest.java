package io.quarkus.signals.deployment.test;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.spi.SignalMetadataEnricher;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.annotation.Identifier;

public class EnricherDuplicateIdTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(FooEnricher.class, BarEnricher.class))
            .setExpectedException(IllegalStateException.class, true);

    @Test
    public void testFailure() {
        fail();
    }

    @Identifier("foo")
    @Singleton
    public static class FooEnricher implements SignalMetadataEnricher {

        @Override
        public void enrich(EnrichmentContext context) {
        }
    }

    @Identifier("foo")
    @Singleton
    public static class BarEnricher implements SignalMetadataEnricher {

        @Override
        public void enrich(EnrichmentContext context) {
        }
    }
}
