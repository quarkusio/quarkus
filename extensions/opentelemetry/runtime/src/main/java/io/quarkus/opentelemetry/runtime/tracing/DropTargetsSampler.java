package io.quarkus.opentelemetry.runtime.tracing;

import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static io.opentelemetry.semconv.UrlAttributes.URL_QUERY;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

public class DropTargetsSampler implements Sampler {
    private final Sampler sampler;
    private final Set<String> dropTargetsExact;
    private final Set<String> dropTargetsWildcard;

    public DropTargetsSampler(final Sampler sampler,
            final Set<String> dropTargets) {
        this.sampler = sampler;
        this.dropTargetsExact = dropTargets.stream().filter(s -> !s.endsWith("*"))
                .collect(Collectors.toCollection(HashSet::new));
        this.dropTargetsWildcard = dropTargets.stream()
                .filter(s -> s.endsWith("*"))
                .map(s -> s.substring(0, s.length() - 1))
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind,
            Attributes attributes, List<LinkData> parentLinks) {

        if (spanKind.equals(SpanKind.SERVER)) {
            // HTTP_TARGET was split into url.path and url.query
            String query = attributes.get(URL_QUERY);
            String target = attributes.get(URL_PATH) + (query == null ? "" : "?" + query);

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
        if (containsExactly(target)) { // check exact match
            return true;
        }
        if (target.charAt(target.length() - 1) == '/') { // check if the path without the ending slash matched
            if (containsExactly(target.substring(0, target.length() - 1))) {
                return true;
            }
        }
        for (String dropTargetsWildcard : dropTargetsWildcard) {
            if (target.startsWith(dropTargetsWildcard)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsExactly(String target) {
        return dropTargetsExact.contains(target);
    }

    @Override
    public String getDescription() {
        return sampler.getDescription();
    }
}
