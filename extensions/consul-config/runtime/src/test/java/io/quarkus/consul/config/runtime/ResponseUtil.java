package io.quarkus.consul.config.runtime;

import java.util.Base64;
import java.util.Optional;

final class ResponseUtil {

    private ResponseUtil() {
    }

    static Response createResponse(String key, String rawValue) {
        return new Response(key, Base64.getEncoder().encodeToString(rawValue.getBytes()));
    }

    static Optional<Response> createOptionalResponse(String key, String rawValue) {
        return Optional.of(createResponse(key, rawValue));
    }
}
