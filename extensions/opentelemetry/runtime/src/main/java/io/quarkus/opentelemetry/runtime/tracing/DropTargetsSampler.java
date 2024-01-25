package io.quarkus.opentelemetry.runtime.tracing;

import java.util.List;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.SemanticAttributes;
import io.quarkus.opentelemetry.runtime.config.runtime.SemconvStabilityType;

public class DropTargetsSampler implements Sampler {
    private final Sampler sampler;
    private final List<String> dropTargets;
    private final SemconvStabilityType semconvStabilityOptin;

    public DropTargetsSampler(final Sampler sampler,
            final List<String> dropTargets,
            final SemconvStabilityType semconvStabilityOptin) {
        this.sampler = sampler;
        this.dropTargets = dropTargets;
        this.semconvStabilityOptin = semconvStabilityOptin;
    }

    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind,
            Attributes attributes, List<LinkData> parentLinks) {

        if (spanKind.equals(SpanKind.SERVER)) {
            String target;
            if (SemconvStabilityType.HTTP.equals(semconvStabilityOptin) ||
                    SemconvStabilityType.HTTP_DUP.equals(semconvStabilityOptin)) {
                // HTTP_TARGET was split into url.path and url.query
                String path = attributes.get(SemanticAttributes.URL_PATH);
                String query = attributes.get(SemanticAttributes.URL_QUERY);
                target = path + (query == null ? "" : "?" + query);
            } else {
                target = attributes.get(SemanticAttributes.HTTP_TARGET);
            }
            if (shouldDrop(target)) {
                return SamplingResult.drop();
            }
        }

        return sampler.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }

    /**
     * Determines whether a path should be dropped
     * TODO: this can certainly be optimized if we find that it's a hot-path
     */
    private boolean shouldDrop(String target) {
        if ((target == null) || target.isEmpty()) {
            return false;
        }
        if (safeContains(target)) { // check exact match
            return true;
        }
        if (target.charAt(target.length() - 1) == '/') { // check if the path without the ending slash matched
            if (safeContains(target.substring(0, target.length() - 1))) {
                return true;
            }
        }
        int lastSlashIndex = target.lastIndexOf('/');
        if (lastSlashIndex != -1) {
            if (safeContains(target.substring(0, lastSlashIndex) + "*")
                    || safeContains(target.substring(0, lastSlashIndex) + "/*")) { // check if a wildcard matches
                return true;
            }
        }
        return false;
    }

    private boolean safeContains(String target) {
        return dropTargets.contains(target);
    }

    @Override
    public String getDescription() {
        return sampler.getDescription();
    }
}
