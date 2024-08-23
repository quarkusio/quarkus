package io.quarkus.analytics.dto.segment;

import java.util.Map;

public interface SegmentContext {
    Map<String, Object> getContext();
}
