package io.quarkus.grpc.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class GrpcTranscodingPathMatchTest {

    @Test
    void exactPathMatches() {
        assertThat(GrpcServerRecorder.matchesTranscodingPath("/v1/items", Set.of("/v1/items"))).isTrue();
    }

    @Test
    void exactPathDoesNotMatchLongerPath() {
        assertThat(GrpcServerRecorder.matchesTranscodingPath("/v1/items-admin", Set.of("/v1/items"))).isFalse();
        assertThat(GrpcServerRecorder.matchesTranscodingPath("/v1/itemsextra", Set.of("/v1/items"))).isFalse();
    }

    @Test
    void parameterizedPrefixMatchesSubPaths() {
        // prefix stored for /v1/items/{id} is /v1/items/
        assertThat(GrpcServerRecorder.matchesTranscodingPath("/v1/items/123", Set.of("/v1/items/"))).isTrue();
        assertThat(GrpcServerRecorder.matchesTranscodingPath("/v1/items/abc/details", Set.of("/v1/items/"))).isTrue();
    }

    @Test
    void parameterizedPrefixDoesNotMatchSiblingPath() {
        assertThat(GrpcServerRecorder.matchesTranscodingPath("/v1/items-admin/123", Set.of("/v1/items/"))).isFalse();
    }

    @Test
    void noMatchWhenPathSetIsEmpty() {
        assertThat(GrpcServerRecorder.matchesTranscodingPath("/v1/items", Set.of())).isFalse();
    }

    @Test
    void matchesAgainstMultiplePrefixes() {
        Set<String> prefixes = Set.of("/v1/items/", "/v2/orders");
        assertThat(GrpcServerRecorder.matchesTranscodingPath("/v1/items/123", prefixes)).isTrue();
        assertThat(GrpcServerRecorder.matchesTranscodingPath("/v2/orders", prefixes)).isTrue();
        assertThat(GrpcServerRecorder.matchesTranscodingPath("/v2/orders-old/1", prefixes)).isFalse();
    }
}
