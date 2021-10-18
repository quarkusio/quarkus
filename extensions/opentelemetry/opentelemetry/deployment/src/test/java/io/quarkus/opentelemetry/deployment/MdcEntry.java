package io.quarkus.opentelemetry.deployment;

import java.util.Objects;

public class MdcEntry {
    public final boolean isSampled;
    public final String parentId;
    public final String spanId;
    public final String traceId;

    public MdcEntry(boolean isSampled, String parentId, String spanId, String traceId) {
        this.isSampled = isSampled;
        this.parentId = parentId;
        this.spanId = spanId;
        this.traceId = traceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MdcEntry)) {
            return false;
        }
        MdcEntry mdcEntry = (MdcEntry) o;
        return isSampled == mdcEntry.isSampled &&
                Objects.equals(parentId, mdcEntry.parentId) &&
                Objects.equals(spanId, mdcEntry.spanId) &&
                Objects.equals(traceId, mdcEntry.traceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isSampled, parentId, spanId, traceId);
    }
}
