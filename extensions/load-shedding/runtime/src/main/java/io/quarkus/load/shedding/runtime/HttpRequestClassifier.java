package io.quarkus.load.shedding.runtime;

import jakarta.inject.Singleton;

import io.quarkus.load.shedding.RequestClassifier;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class HttpRequestClassifier implements RequestClassifier<RoutingContext> {
    // 2^32 / Phi, the golden ratio constant commonly used for hashing
    private static final int GOLDEN_RATIO = 0x9E3779B9;

    @Override
    public boolean appliesTo(Object request) {
        return request instanceof RoutingContext;
    }

    @Override
    public int cohort(RoutingContext request) {
        int hour = (int) (System.currentTimeMillis() >> 22); // roughly 1 hour
        SocketAddress remoteAddress = request.request().remoteAddress();
        String host = remoteAddress == null ? null : remoteAddress.hostAddress();
        if (host == null) {
            host = "";
        }
        return cohort(host, hour);
    }

    static int cohort(String host, int hour) {
        return fmix(host.hashCode() ^ (hour * GOLDEN_RATIO)) & RequestClassifier.maxCohort();
    }

    // Only the lowest 7 bits survive the masking above, and neither `String.hashCode()` nor multiplication
    // puts much information there, because carries propagate towards the higher bits. Addresses from one
    // subnet hash to nearly the same number, so without mixing, they end up in a handful of adjacent cohorts,
    // leaving a lot of cohorts empty and skewing how much load priority shedding actually sheds.
    //
    // This is the Murmur3 finalizer: the shifts fold the higher bits down onto the lower ones and
    // the multiplications spread them back up, so that every input bit affects every output bit and
    // the lowest 7 bits are as good as any other.
    private static int fmix(int hash) {
        hash ^= hash >>> 16;
        hash *= 0x85EBCA6B;
        hash ^= hash >>> 13;
        hash *= 0xC2B2AE35;
        hash ^= hash >>> 16;
        return hash;
    }
}
