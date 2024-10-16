package io.quarkus.opentelemetry.runtime.tracing;

import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
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

class DropTargetsSamplerTest {

    @Test
    void testDropTargets() {
        CountingSampler countingSampler = new CountingSampler();
        var sut = new DropTargetsSampler(countingSampler, List.of("/q/swagger-ui", "/q/swagger-ui*"));

        assertEquals(SamplingResult.recordAndSample(), getShouldSample(sut, "/other"));
        assertEquals(1, countingSampler.count.get());

        assertEquals(SamplingResult.drop(), getShouldSample(sut, "/q/swagger-ui"));
        assertEquals(1, countingSampler.count.get());

        assertEquals(SamplingResult.drop(), getShouldSample(sut, "/q/swagger-ui/"));
        assertEquals(1, countingSampler.count.get());

        assertEquals(SamplingResult.drop(), getShouldSample(sut, "/q/swagger-ui/whatever"));
        assertEquals(1, countingSampler.count.get());

        assertEquals(SamplingResult.recordAndSample(), getShouldSample(sut, "/q/test"));
        assertEquals(2, countingSampler.count.get());
    }

    private static SamplingResult getShouldSample(DropTargetsSampler sut, String target) {
        return sut.shouldSample(null, null, null, SpanKind.SERVER,
                Attributes.of(URL_PATH, target), null);
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
