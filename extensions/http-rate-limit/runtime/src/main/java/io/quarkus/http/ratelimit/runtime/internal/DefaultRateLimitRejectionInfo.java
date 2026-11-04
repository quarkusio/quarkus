package io.quarkus.http.ratelimit.runtime.internal;

import java.util.Optional;

import io.quarkus.http.ratelimit.RateLimitPhase;
import io.quarkus.http.ratelimit.RateLimitRejectionInfo;

record DefaultRateLimitRejectionInfo(String rule, RateLimitPhase phase, String key, long retryAfterSeconds,
        String method, String path, String remoteAddress,
        Optional<String> principal) implements RateLimitRejectionInfo {
}
