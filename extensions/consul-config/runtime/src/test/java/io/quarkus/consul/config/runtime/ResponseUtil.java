package io.quarkus.consul.config.runtime;

import java.util.Base64;

import io.smallrye.mutiny.Uni;

final class ResponseUtil {

    private ResponseUtil() {
    }

    static Uni<Response> validResponse(String key, String rawValue) {
        return Uni.createFrom().item(new Response(key, Base64.getEncoder().encodeToString(rawValue.getBytes())));
    }

    static Uni<Response> emptyResponse() {
        return Uni.createFrom().nullItem();
    }
}
