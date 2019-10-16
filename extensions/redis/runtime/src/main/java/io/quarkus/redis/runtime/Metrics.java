package io.quarkus.redis.runtime;

import io.lettuce.core.internal.LettuceClassUtils;

public class Metrics {
    public static boolean enabled() {
        return LettuceClassUtils.isPresent("org.LatencyUtils.PauseDetector")
                && LettuceClassUtils.isPresent("org.HdrHistogram.Histogram");
    }
}
