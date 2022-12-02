package io.quarkus.opentelemetry.runtime.tracing;

import java.util.List;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

public class DropTargetsSampler implements Sampler {
    private final Sampler sampler;
    private final List<String> dropTargets;

    public DropTargetsSampler(Sampler sampler, List<String> dropTargets) {
        this.sampler = sampler;
        this.dropTargets = dropTargets;
    }

    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind,
            Attributes attributes, List<LinkData> parentLinks) {

        if (spanKind.equals(SpanKind.SERVER)) {
            String target = attributes.get(SemanticAttributes.HTTP_TARGET);
            // TODO - radcortez - Match /* endpoints
            if (target != null && dropTargets.contains(target)) {
                return SamplingResult.drop();
            }
        }

        return sampler.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }

    @Override
    public String getDescription() {
        return sampler.getDescription();
    }
}
