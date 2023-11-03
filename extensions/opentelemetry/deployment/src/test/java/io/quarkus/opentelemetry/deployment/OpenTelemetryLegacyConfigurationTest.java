package io.quarkus.opentelemetry.deployment;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.config.build.exporter.OtlpExporterBuildConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.SmallRyeConfig;

class OpenTelemetryLegacyConfigurationTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.opentelemetry.enabled", "false")
            .overrideConfigKey("quarkus.opentelemetry.tracer.enabled", "false")
            .overrideConfigKey("quarkus.opentelemetry.propagators", "tracecontext")
            .overrideConfigKey("quarkus.opentelemetry.tracer.resource-attributes", "service.name=authservice")
            .overrideConfigKey("quarkus.opentelemetry.tracer.suppress-non-application-uris", "false")
            .overrideConfigKey("quarkus.opentelemetry.tracer.include-static-resources", "true")
            .overrideConfigKey("quarkus.opentelemetry.tracer.sampler", "off")
            .overrideConfigKey("quarkus.opentelemetry.tracer.sampler.ratio", "2.0d")
            .overrideConfigKey("quarkus.opentelemetry.tracer.exporter.otlp.headers", "header=value")
            .overrideConfigKey("quarkus.opentelemetry.tracer.exporter.otlp.enabled", "false")
            .overrideConfigKey("quarkus.opentelemetry.tracer.exporter.otlp.endpoint", "http://localhost:4318/");

    @Inject
    OTelBuildConfig oTelBuildConfig;
    @Inject
    OTelRuntimeConfig oTelRuntimeConfig;
    @Inject
    OtlpExporterBuildConfig otlpExporterBuildConfig;
    @Inject
    OtlpExporterRuntimeConfig otlpExporterRuntimeConfig;
    @Inject
    SmallRyeConfig config;

    @Test
    void config() {
        assertEquals(FALSE, oTelBuildConfig.enabled());
        assertTrue(oTelBuildConfig.traces().enabled().isPresent());
        assertEquals(FALSE, oTelBuildConfig.traces().enabled().get());
        assertEquals(List.of("tracecontext"), oTelBuildConfig.propagators()); // will not include the default baggagge
        assertTrue(oTelRuntimeConfig.resourceAttributes().isPresent());
        assertEquals("service.name=authservice", oTelRuntimeConfig.resourceAttributes().get().get(0));
        assertEquals(FALSE, oTelRuntimeConfig.traces().suppressNonApplicationUris());
        assertEquals(TRUE, oTelRuntimeConfig.traces().includeStaticResources());
        assertEquals("always_off", oTelBuildConfig.traces().sampler());
        assertTrue(oTelRuntimeConfig.traces().samplerArg().isPresent());
        assertEquals("2.0d", oTelRuntimeConfig.traces().samplerArg().get());
        assertEquals(FALSE, otlpExporterBuildConfig.enabled());
        assertTrue(otlpExporterRuntimeConfig.traces().legacyEndpoint().isPresent());
        assertTrue(otlpExporterRuntimeConfig.traces().headers().isPresent());
        assertEquals("header=value", otlpExporterRuntimeConfig.traces().headers().get().get(0));
        assertEquals("http://localhost:4318/", otlpExporterRuntimeConfig.traces().legacyEndpoint().get());
    }

    @Test
    void names() {
        assertTrue(config.isPropertyPresent("quarkus.otel.enabled"));
        assertTrue(config.isPropertyPresent("quarkus.otel.metrics.exporter"));
        assertTrue(config.isPropertyPresent("quarkus.otel.propagators"));
        assertTrue(config.isPropertyPresent("quarkus.otel.logs.exporter"));
        assertTrue(config.isPropertyPresent("quarkus.otel.traces.enabled"));
        assertTrue(config.isPropertyPresent("quarkus.otel.traces.exporter"));
        assertTrue(config.isPropertyPresent("quarkus.otel.traces.sampler"));
        assertTrue(config.isPropertyPresent("quarkus.otel.sdk.disabled"));
        assertTrue(config.isPropertyPresent("quarkus.otel.service.name"));
        assertTrue(config.isPropertyPresent("quarkus.otel.attribute.count.limit"));
        assertTrue(config.isPropertyPresent("quarkus.otel.span.attribute.count.limit"));
        assertTrue(config.isPropertyPresent("quarkus.otel.span.event.count.limit"));
        assertTrue(config.isPropertyPresent("quarkus.otel.span.link.count.limit"));
        assertTrue(config.isPropertyPresent("quarkus.otel.bsp.schedule.delay"));
        assertTrue(config.isPropertyPresent("quarkus.otel.bsp.max.queue.size"));
        assertTrue(config.isPropertyPresent("quarkus.otel.bsp.max.export.batch.size"));
        assertTrue(config.isPropertyPresent("quarkus.otel.bsp.export.timeout"));
    }
}
