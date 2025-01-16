package io.quarkus.opentelemetry.runtime.tracing;

import java.util.List;
import java.util.Set;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.SemanticAttributes;

public class DropTargetsSampler implements Sampler {
    private final Sampler sampler;
    private final Set<String> dropTargets;

    public DropTargetsSampler(final Sampler sampler,
            final Set<String> dropTargets) {
        this.sampler = sampler;
        this.dropTargets = dropTargets;
    }

    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind,
            Attributes attributes, List<LinkData> parentLinks) {

        if (spanKind.equals(SpanKind.SERVER)) {
            // HTTP_TARGET was split into url.path and url.query
            String path = attributes.get(SemanticAttributes.URL_PATH);
            String query = attributes.get(SemanticAttributes.URL_QUERY);
            String target = path + (query == null ? "" : "?" + query);

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
