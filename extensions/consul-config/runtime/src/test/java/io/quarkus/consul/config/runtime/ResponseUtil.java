package io.quarkus.consul.config.runtime;

import java.util.Base64;
import java.util.List;

import io.smallrye.mutiny.Uni;

final class ResponseUtil {

    private ResponseUtil() {
    }

    static Uni<Response> validResponse(String key, String rawValue) {
        return Uni.createFrom().item(encodeResponse(key, rawValue));
    }

    static Uni<MultiResponse> validMultiResponse(List<String> keys, List<String> rawValues) {
        MultiResponse multiResponse = new MultiResponse();
        for (int i = 0; i < keys.size(); i++) {
            multiResponse.addResponse(encodeResponse(keys.get(i), rawValues.get(i)));
        }
        return Uni.createFrom().item(multiResponse);
    }

    private static Response encodeResponse(String key, String rawValue) {
        return new Response(key, Base64.getEncoder().encodeToString(rawValue.getBytes()));
    }

    static Uni<Response> emptyResponse() {
        return Uni.createFrom().nullItem();
    }
}
