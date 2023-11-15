package io.quarkus.it.opentelemetry.spi;

import java.util.List;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.SemanticAttributes;

public class CustomSPISampler implements Sampler {
    @Override
    public SamplingResult shouldSample(Context context,
            String s,
            String s1,
            SpanKind spanKind,
            Attributes attributes,
            List<LinkData> list) {
        if (attributes.get(SemanticAttributes.HTTP_TARGET).startsWith("/param/")) {
            return SamplingResult.drop();
        }
        return Sampler.alwaysOn().shouldSample(context, s, s1, spanKind, attributes, list);
    }

    @Override
    public String getDescription() {
        return "custom-spi-sampler";
    }
}
