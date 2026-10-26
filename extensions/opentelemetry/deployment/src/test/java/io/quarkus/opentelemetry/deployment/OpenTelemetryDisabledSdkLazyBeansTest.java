package io.quarkus.opentelemetry.deployment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * With the SDK disabled, no injected component bean is created and runtime configuration values the enabled path
 * rejects are ignored, as on the {@code AutoConfiguredOpenTelemetrySdk} path, which skipped all component
 * configuration when disabled.
 */
public class OpenTelemetryDisabledSdkLazyBeansTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TrackedSpanProcessor.class))
            .overrideConfigKey("quarkus.otel.sdk.disabled", "true")
            // rejected by the model builder on the enabled path; the disabled path must not interpret it
            .overrideConfigKey("quarkus.otel.traces.sampler.arg", "not-a-number")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false");

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void beansAreNotCreatedAndInvalidRuntimeConfigIsIgnored() {
        // forces the synthetic OpenTelemetry bean; the application started despite the invalid sampler argument
        assertNotNull(openTelemetry.getTracer("test"));
        assertFalse(TrackedSpanProcessor.created,
                "A SpanProcessor bean must not be created when the SDK is disabled");
    }

    @Singleton
    public static class TrackedSpanProcessor implements SpanProcessor {

        static volatile boolean created;

        public TrackedSpanProcessor() {
            created = true;
        }

        @Override
        public void onStart(Context parentContext, ReadWriteSpan span) {
        }

        @Override
        public boolean isStartRequired() {
            return false;
        }

        @Override
        public void onEnd(ReadableSpan span) {
        }

        @Override
        public boolean isEndRequired() {
            return false;
        }
    }
}
