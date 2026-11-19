package io.quarkus.http.ratelimit.runtime.internal;

import java.util.Optional;

import io.quarkus.http.ratelimit.RateLimit;
import io.quarkus.http.ratelimit.RateLimitPhase;
import io.quarkus.http.ratelimit.RateLimitRejectionInfo;

record DefaultRateLimitRejectionInfo(String rule, RateLimitPhase phase, Optional<String> key, long retryAfterSeconds,
        String method, String path, String remoteAddress,
        Optional<String> principal, Reason reason, Optional<RateLimit> limit) implements RateLimitRejectionInfo {
}
