package io.quarkus.grpc.runtime.transcoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.grpc.transcoding.GrpcTranscodingHttpUtils;

/**
 * Tests for {@link GrpcTranscodingHttpUtils}. In this test class, we test the utility methods that are used to match
 * HTTP paths to gRPC methods. This should cover most of the cases which can be encountered in the real world.
 */
class GrpcTranscodingHttpUtilsTest {

    @Test
    @DisplayName("Test isPathMatch")
    void testIsPathMatchSimple() {
        assertTrue(GrpcTranscodingHttpUtils.isPathMatch("/hello", "/hello"));
    }

    @Test
    @DisplayName("Test isPathMatch with single variable")
    void testIsPathMatchSingleVariable() {
        assertTrue(GrpcTranscodingHttpUtils.isPathMatch("/hello/{name}", "/hello/world"));
    }

    @Test
    @DisplayName("Test isPathMatch with multiple variables")
    void testIsPathMatchMultipleVariables() {
        assertTrue(GrpcTranscodingHttpUtils.isPathMatch("/users/{id}/posts/{postId}", "/users/123/posts/456"));
    }

    @Test
    @DisplayName("Test isPathMatch with mismatch")
    void testIsPathMatchMismatch() {
        assertFalse(GrpcTranscodingHttpUtils.isPathMatch("/hello/{name}", "/goodbye/world"));
    }

    @Test
    @DisplayName("Test isPathMatch with path length mismatch")
    void testIsPathMatchPathLengthMismatch() {
        assertFalse(GrpcTranscodingHttpUtils.isPathMatch("/hello", "/hello/world"));
    }

    @Test
    @DisplayName("Test isPathMatch with template length mismatch")
    void testIsPathMatchEmptyVariableSegment() {
        assertFalse(GrpcTranscodingHttpUtils.isPathMatch("/items/{id}", "/items/"));
    }

    @Test
    @DisplayName("Test isPathMatch with empty paths")
    void testIsPathMatchEmptyPaths() {
        assertTrue(GrpcTranscodingHttpUtils.isPathMatch("", ""));
    }

    @Test
    @DisplayName("Test isPathMatch with empty template")
    void testIsPathMatchOnlyVariableSegments() {
        assertTrue(GrpcTranscodingHttpUtils.isPathMatch("/{id}/{name}", "/123/John"));
    }

    @Test
    @DisplayName("Test extractPathParams with single parameter")
    void testExtractPathParamsSingleParameter() {
        Map<String, String> expected = new HashMap<>();
        expected.put("id", "123");
        assertEquals(expected, GrpcTranscodingHttpUtils.extractPathParams("/items/{id}", "/items/123"));
    }

    @Test
    @DisplayName("Test extractPathParams with multiple parameters")
    void testExtractPathParamsMultipleParameters() {
        Map<String, String> expected = new HashMap<>();
        expected.put("userId", "5");
        expected.put("postId", "87");
        assertEquals(expected,
                GrpcTranscodingHttpUtils.extractPathParams("/users/{userId}/posts/{postId}", "/users/5/posts/87"));
    }

    @Test
    @DisplayName("Test extractPathParams with empty parameters")
    void testExtractPathParamsNoParameters() {
        assertTrue(GrpcTranscodingHttpUtils.extractPathParams("/hello", "/hello").isEmpty());
    }

    @Test
    @DisplayName("Test extractPathParams with extra segments")
    void testExtractPathParamsExtraSegments() {
        assertEquals(Map.of(), GrpcTranscodingHttpUtils.extractPathParams("/items/{id}", "/items/123/extra"));
    }
}
