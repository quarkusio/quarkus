package io.quarkus.opentelemetry.runtime.tracing;

import static io.quarkus.opentelemetry.runtime.OpenTelemetryUtil.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.SemanticAttributes;
import io.quarkus.opentelemetry.runtime.config.runtime.SemconvStabilityType;

class DropTargetsSamplerTest {

    @Test
    void testDropTargets() {
        CountingSampler countingSampler = new CountingSampler();
        SemconvStabilityType semconvStabilityOptin = getSemconvStabilityOptin(
                System.getProperty("quarkus.otel.semconv-stability.opt-in"));
        var sut = new DropTargetsSampler(countingSampler, List.of("/q/swagger-ui", "/q/swagger-ui*"),
                semconvStabilityOptin);

        assertEquals(SamplingResult.recordAndSample(), getShouldSample(sut, "/other", semconvStabilityOptin));
        assertEquals(1, countingSampler.count.get());

        assertEquals(SamplingResult.drop(), getShouldSample(sut, "/q/swagger-ui", semconvStabilityOptin));
        assertEquals(1, countingSampler.count.get());

        assertEquals(SamplingResult.drop(), getShouldSample(sut, "/q/swagger-ui/", semconvStabilityOptin));
        assertEquals(1, countingSampler.count.get());

        assertEquals(SamplingResult.drop(), getShouldSample(sut, "/q/swagger-ui/whatever", semconvStabilityOptin));
        assertEquals(1, countingSampler.count.get());

        assertEquals(SamplingResult.recordAndSample(), getShouldSample(sut, "/q/test", semconvStabilityOptin));
        assertEquals(2, countingSampler.count.get());
    }

    private static SamplingResult getShouldSample(DropTargetsSampler sut,
            String target,
            SemconvStabilityType semconvStabilityOptin) {
        if (SemconvStabilityType.HTTP.equals(semconvStabilityOptin) ||
                SemconvStabilityType.HTTP_DUP.equals(semconvStabilityOptin)) {
            return sut.shouldSample(null, null, null, SpanKind.SERVER,
                    Attributes.of(SemanticAttributes.URL_PATH, target), null);
        }
        return sut.shouldSample(null, null, null, SpanKind.SERVER,
                Attributes.of(SemanticAttributes.HTTP_TARGET, target), null);
    }

    private static final class CountingSampler implements Sampler {

        final AtomicLong count = new AtomicLong(0);

        @Override
        public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind,
                Attributes attributes, List<LinkData> parentLinks) {
            count.incrementAndGet();
            return SamplingResult.recordAndSample();
        }

        @Override
        public String getDescription() {
            return "test";
        }
    }
}
