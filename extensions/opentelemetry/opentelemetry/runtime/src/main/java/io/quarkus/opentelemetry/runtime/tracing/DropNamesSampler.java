package io.quarkus.opentelemetry.runtime.tracing;

import java.util.List;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

public class DropNamesSampler implements Sampler {
    private final Sampler sampler;
    private final List<String> dropNames;

    public DropNamesSampler(Sampler sampler, List<String> dropNames) {
        this.sampler = sampler;
        this.dropNames = dropNames;
    }

    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind,
            Attributes attributes, List<LinkData> parentLinks) {

        if (spanKind.equals(SpanKind.SERVER)) {
            for (String dropName : dropNames) {
                if (name.startsWith(dropName)) {
                    return SamplingResult.drop();
                }
            }
        }

        return sampler.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }

    @Override
    public String getDescription() {
        return sampler.getDescription();
    }
}
