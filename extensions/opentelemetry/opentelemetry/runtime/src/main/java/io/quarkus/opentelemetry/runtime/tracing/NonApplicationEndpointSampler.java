package io.quarkus.opentelemetry.runtime.tracing;

import java.util.List;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

public class NonApplicationEndpointSampler implements Sampler {
    private static final SamplingResult NEGATIVE_SAMPLING_RESULT = SamplingResult.create(SamplingDecision.DROP);

    private final Sampler root;

    public NonApplicationEndpointSampler(Sampler root) {
        this.root = root;
    }

    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind,
            Attributes attributes, List<LinkData> parentLinks) {
        if (name.startsWith("q/") && spanKind == SpanKind.SERVER) {
            return NEGATIVE_SAMPLING_RESULT;
        }
        return root.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }

    @Override
    public String getDescription() {
        return "NonApplicationEndpointBased{/q}";
    }
}
