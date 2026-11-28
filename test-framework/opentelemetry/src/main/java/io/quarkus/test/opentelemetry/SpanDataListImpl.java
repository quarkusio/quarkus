package io.quarkus.test.opentelemetry;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.data.SpanData;

final class SpanDataListImpl extends AbstractList<SpanData> implements SpanDataList {

    private final List<SpanData> spans;

    SpanDataListImpl(List<SpanData> spans) {
        this.spans = List.copyOf(spans);
    }

    @Override
    public SpanData get(int index) {
        return spans.get(index);
    }

    @Override
    public int size() {
        return spans.size();
    }

    @Override
    public SpanDataList ofKind(SpanKind kind) {
        return filter(span -> span.getKind() == kind);
    }

    @Override
    public SpanDataList named(String name) {
        return filter(span -> span.getName().equals(name));
    }

    @Override
    public SpanDataList roots() {
        return filter(span -> !span.getParentSpanContext().isValid());
    }

    @Override
    public SpanDataList childrenOf(SpanData parent) {
        return filter(span -> span.getTraceId().equals(parent.getTraceId())
                && span.getParentSpanId().equals(parent.getSpanId()));
    }

    @Override
    public SpanDataList filter(Predicate<SpanData> predicate) {
        List<SpanData> result = new ArrayList<>();
        for (SpanData span : spans) {
            if (predicate.test(span)) {
                result.add(span);
            }
        }
        return new SpanDataListImpl(result);
    }

    @Override
    public SpanData single() {
        if (spans.size() != 1) {
            throw new AssertionError("Expected exactly one span but found " + this);
        }
        return spans.getFirst();
    }

    /**
     * One line for each span, for the failure messages.
     */
    @Override
    public String toString() {
        if (spans.isEmpty()) {
            return "no spans";
        }
        StringBuilder result = new StringBuilder().append(spans.size()).append(" span(s):");
        for (SpanData span : spans) {
            result.append(System.lineSeparator()).append("  ")
                    .append(span.getKind()).append(" '").append(span.getName())
                    .append("' (trace ").append(span.getTraceId())
                    .append(", span ").append(span.getSpanId())
                    .append(", parent ").append(span.getParentSpanId()).append(")");
        }
        return result.toString();
    }
}
