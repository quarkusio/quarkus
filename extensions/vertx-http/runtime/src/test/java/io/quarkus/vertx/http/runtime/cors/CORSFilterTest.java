package io.quarkus.vertx.http.runtime.cors;

import static io.quarkus.vertx.http.runtime.cors.CORSFilter.isConfiguredWithWildcard;
import static io.quarkus.vertx.http.runtime.cors.CORSFilter.isOriginAllowedByRegex;
import static io.quarkus.vertx.http.runtime.cors.CORSFilter.isSameOrigin;
import static io.quarkus.vertx.http.runtime.cors.CORSFilter.parseAllowedOriginsRegex;
import static io.quarkus.vertx.http.runtime.cors.CORSFilter.substringMatch;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.vertx.http.security.CORS;
import io.vertx.core.http.HttpServerRequest;

public class CORSFilterTest {

    @Test
    public void isConfiguredWithWildcardTest() {
        Assertions.assertTrue(isConfiguredWithWildcard(Optional.empty()));
        Assertions.assertTrue(isConfiguredWithWildcard(Optional.of(Collections.EMPTY_LIST)));
        Assertions.assertTrue(isConfiguredWithWildcard(Optional.of(Collections.singletonList("*"))));

        Assertions.assertFalse(isConfiguredWithWildcard(Optional.of(Arrays.asList("PUT", "GET", "POST"))));
        Assertions.assertFalse(isConfiguredWithWildcard(Optional.of(Arrays.asList("http://localhost:8080/", "*"))));
        Assertions.assertFalse(isConfiguredWithWildcard(Optional.of(Collections.singletonList("http://localhost:8080/"))));
    }

    @Test
    public void isOriginAllowedByRegexTest() {
        Assertions.assertFalse(isOriginAllowedByRegex(Collections.emptyList(), "http://locahost:8080"));
        Assertions.assertEquals(0,
                parseAllowedOriginsRegex(Optional.of(Collections.singletonList("http://localhost:8080"))).size());
        List<Pattern> regexList = parseAllowedOriginsRegex(
                Optional.of(Collections.singletonList("/https://([a-z0-9\\-_]+)\\.app\\.mydomain\\.com/")));
        Assertions.assertEquals(1, regexList.size());
        Assertions.assertTrue(isOriginAllowedByRegex(regexList, "https://abc-123.app.mydomain.com"));
        Assertions.assertFalse(isOriginAllowedByRegex(regexList, "https://abc-123app.mydomain.com"));
    }

    @Test
    public void sameOriginTest() {
        var request = Mockito.mock(HttpServerRequest.class);
        Mockito.when(request.scheme()).thenReturn("http");
        Mockito.when(request.host()).thenReturn("localhost");
        Mockito.when(request.absoluteURI()).thenReturn("http://localhost");
        Assertions.assertTrue(isSameOrigin(request, "http://localhost"));
        Assertions.assertTrue(isSameOrigin(request, "http://localhost:80"));
        Assertions.assertFalse(isSameOrigin(request, "http://localhost:8080"));
        Assertions.assertFalse(isSameOrigin(request, "https://localhost"));
        Mockito.when(request.host()).thenReturn("localhost:8080");
        Mockito.when(request.absoluteURI()).thenReturn("http://localhost:8080");
        Assertions.assertFalse(isSameOrigin(request, "http://localhost"));
        Assertions.assertFalse(isSameOrigin(request, "http://localhost:80"));
        Assertions.assertTrue(isSameOrigin(request, "http://localhost:8080"));
        Assertions.assertFalse(isSameOrigin(request, "https://localhost:8080"));
        Mockito.when(request.scheme()).thenReturn("https");
        Mockito.when(request.host()).thenReturn("localhost");
        Mockito.when(request.absoluteURI()).thenReturn("http://localhost");
        Assertions.assertFalse(isSameOrigin(request, "http://localhost"));
        Assertions.assertFalse(isSameOrigin(request, "http://localhost:443"));
        Assertions.assertFalse(isSameOrigin(request, "https://localhost:8080"));
        Assertions.assertTrue(isSameOrigin(request, "https://localhost"));
        Mockito.when(request.host()).thenReturn("localhost:8443");
        Mockito.when(request.absoluteURI()).thenReturn("https://localhost:8443");
        Assertions.assertFalse(isSameOrigin(request, "http://localhost"));
        Assertions.assertFalse(isSameOrigin(request, "http://localhost:80"));
        Assertions.assertFalse(isSameOrigin(request, "http://localhost:8443"));
        Assertions.assertTrue(isSameOrigin(request, "https://localhost:8443"));
        Assertions.assertFalse(isSameOrigin(request, "http://%s"));

    }

    @Test
    public void sameOriginPublicWebAddressTest() {
        var request = Mockito.mock(HttpServerRequest.class);
        Mockito.when(request.scheme()).thenReturn("https");
        Mockito.when(request.host()).thenReturn("stage.code.quarkus.io");
        Mockito.when(request.absoluteURI()).thenReturn("https://stage.code.quarkus.io/api/project");
        Assertions.assertFalse(isSameOrigin(request, "http://localhost"));
        Assertions.assertFalse(isSameOrigin(request, "https://code.quarkus.io"));
        Assertions.assertTrue(isSameOrigin(request, "https://stage.code.quarkus.io"));
    }

    @Test
    public void testSubstringMatches() {
        Assertions.assertTrue(substringMatch("localhost", 0, "local", false));
        Assertions.assertFalse(substringMatch("localhost", 0, "local", true));
        Assertions.assertFalse(substringMatch("localhost", 1, "local", false));
        Assertions.assertTrue(substringMatch("localhost", 5, "host", false));
        Assertions.assertTrue(substringMatch("localhost", 5, "host", true));

    }

    @Test
    void testCorsConfig() {
        record CORSConfigImpl(Optional<Boolean> accessControlAllowCredentials, Optional<Duration> accessControlMaxAge,
                Optional<List<String>> exposedHeaders, Optional<List<String>> headers,
                Optional<List<String>> methods, Optional<List<String>> origins) implements CORSConfig {
            @Override
            public boolean enabled() {
                return true;
            }
        }
        var config = new CORSConfigImpl(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());
        var emptyConfig = (CORSConfig) new CORS.Builder(config).build();
        Assertions.assertTrue(emptyConfig.enabled());
        Assertions.assertTrue(emptyConfig.origins().isEmpty());
        Assertions.assertTrue(emptyConfig.headers().isEmpty());
        Assertions.assertTrue(emptyConfig.exposedHeaders().isEmpty());
        Assertions.assertTrue(emptyConfig.methods().isEmpty());
        Assertions.assertTrue(emptyConfig.accessControlMaxAge().isEmpty());
        Assertions.assertTrue(emptyConfig.accessControlAllowCredentials().isEmpty());
        var configWithOriginsOnly = (CORSConfig) new CORS.Builder(config)
                .origin("origin-1")
                .origins(Set.of("origin-2", "origin-3"))
                .origins(Set.of("origin-4", "origin-5"))
                .build();
        Assertions.assertTrue(configWithOriginsOnly.enabled());
        Assertions.assertTrue(configWithOriginsOnly.origins().isPresent());
        List<String> origins = configWithOriginsOnly.origins().get();
        assertThat(origins)
                .size().isEqualTo(5)
                .returnToIterable()
                .contains("origin-1", "origin-2", "origin-3", "origin-4", "origin-5");
        Assertions.assertTrue(configWithOriginsOnly.headers().isEmpty());
        Assertions.assertTrue(configWithOriginsOnly.exposedHeaders().isEmpty());
        Assertions.assertTrue(configWithOriginsOnly.methods().isEmpty());
        Assertions.assertTrue(configWithOriginsOnly.accessControlMaxAge().isEmpty());
        Assertions.assertTrue(configWithOriginsOnly.accessControlAllowCredentials().isEmpty());
        var configWithMethodsOnly = (CORSConfig) new CORS.Builder(config)
                .method("method-1")
                .methods(Set.of("method-2", "method-3"))
                .methods(Set.of("method-4", "method-5"))
                .build();
        Assertions.assertTrue(configWithMethodsOnly.enabled());
        Assertions.assertTrue(configWithMethodsOnly.methods().isPresent());
        List<String> methods = configWithMethodsOnly.methods().get();
        assertThat(methods)
                .size().isEqualTo(5)
                .returnToIterable()
                .contains("method-1", "method-2", "method-3", "method-4", "method-5");
        Assertions.assertTrue(configWithMethodsOnly.headers().isEmpty());
        Assertions.assertTrue(configWithMethodsOnly.exposedHeaders().isEmpty());
        Assertions.assertTrue(configWithMethodsOnly.origins().isEmpty());
        Assertions.assertTrue(configWithMethodsOnly.accessControlMaxAge().isEmpty());
        Assertions.assertTrue(configWithMethodsOnly.accessControlAllowCredentials().isEmpty());
        var configWithHeadersOnly = (CORSConfig) new CORS.Builder(config)
                .header("header-1")
                .headers(Set.of("header-2", "header-3"))
                .headers(Set.of("header-4", "header-5"))
                .build();
        Assertions.assertTrue(configWithHeadersOnly.enabled());
        Assertions.assertTrue(configWithHeadersOnly.headers().isPresent());
        List<String> headers = configWithHeadersOnly.headers().get();
        assertThat(headers)
                .size().isEqualTo(5)
                .returnToIterable()
                .contains("header-1", "header-2", "header-3", "header-4", "header-5");
        Assertions.assertTrue(configWithHeadersOnly.methods().isEmpty());
        Assertions.assertTrue(configWithHeadersOnly.exposedHeaders().isEmpty());
        Assertions.assertTrue(configWithHeadersOnly.origins().isEmpty());
        Assertions.assertTrue(configWithHeadersOnly.accessControlMaxAge().isEmpty());
        Assertions.assertTrue(configWithHeadersOnly.accessControlAllowCredentials().isEmpty());
        var configWithExposedHeadersOnly = (CORSConfig) new CORS.Builder(config)
                .exposedHeader("header-1")
                .exposedHeaders(Set.of("header-2", "header-3"))
                .exposedHeaders(Set.of("header-4", "header-5"))
                .build();
        Assertions.assertTrue(configWithExposedHeadersOnly.enabled());
        Assertions.assertTrue(configWithExposedHeadersOnly.exposedHeaders().isPresent());
        List<String> exposedHeaders = configWithExposedHeadersOnly.exposedHeaders().get();
        assertThat(exposedHeaders)
                .size().isEqualTo(5)
                .returnToIterable()
                .contains("header-1", "header-2", "header-3", "header-4", "header-5");
        Assertions.assertTrue(configWithExposedHeadersOnly.methods().isEmpty());
        Assertions.assertTrue(configWithExposedHeadersOnly.headers().isEmpty());
        Assertions.assertTrue(configWithExposedHeadersOnly.origins().isEmpty());
        Assertions.assertTrue(configWithExposedHeadersOnly.accessControlMaxAge().isEmpty());
        Assertions.assertTrue(configWithExposedHeadersOnly.accessControlAllowCredentials().isEmpty());
        var configWithEnabledControlAllowCredentials = (CORSConfig) new CORS.Builder(config)
                .accessControlAllowCredentials().build();
        Assertions.assertTrue(configWithEnabledControlAllowCredentials.accessControlAllowCredentials().isPresent());
        Assertions.assertTrue(configWithEnabledControlAllowCredentials.accessControlAllowCredentials().get());
        Assertions.assertTrue(configWithEnabledControlAllowCredentials.enabled());
        Assertions.assertTrue(configWithEnabledControlAllowCredentials.methods().isEmpty());
        Assertions.assertTrue(configWithEnabledControlAllowCredentials.headers().isEmpty());
        Assertions.assertTrue(configWithEnabledControlAllowCredentials.origins().isEmpty());
        Assertions.assertTrue(configWithEnabledControlAllowCredentials.accessControlMaxAge().isEmpty());
        Assertions.assertTrue(configWithEnabledControlAllowCredentials.exposedHeaders().isEmpty());
        var configWithDisabledControlAllowCredentials = (CORSConfig) new CORS.Builder(config)
                .accessControlAllowCredentials(false).build();
        Assertions.assertTrue(configWithDisabledControlAllowCredentials.accessControlAllowCredentials().isPresent());
        Assertions.assertFalse(configWithDisabledControlAllowCredentials.accessControlAllowCredentials().get());
        Assertions.assertTrue(configWithDisabledControlAllowCredentials.enabled());
        Assertions.assertTrue(configWithDisabledControlAllowCredentials.methods().isEmpty());
        Assertions.assertTrue(configWithDisabledControlAllowCredentials.headers().isEmpty());
        Assertions.assertTrue(configWithDisabledControlAllowCredentials.origins().isEmpty());
        Assertions.assertTrue(configWithDisabledControlAllowCredentials.accessControlMaxAge().isEmpty());
        Assertions.assertTrue(configWithDisabledControlAllowCredentials.exposedHeaders().isEmpty());
        var configWithAccessControlMaxAge = (CORSConfig) new CORS.Builder(config).accessControlMaxAge(Duration.ofDays(2))
                .build();
        Assertions.assertTrue(configWithAccessControlMaxAge.accessControlMaxAge().isPresent());
        Assertions.assertEquals(2, configWithAccessControlMaxAge.accessControlMaxAge().get().toDays());
        Assertions.assertTrue(configWithAccessControlMaxAge.enabled());
        Assertions.assertTrue(configWithAccessControlMaxAge.methods().isEmpty());
        Assertions.assertTrue(configWithAccessControlMaxAge.headers().isEmpty());
        Assertions.assertTrue(configWithAccessControlMaxAge.origins().isEmpty());
        Assertions.assertTrue(configWithAccessControlMaxAge.accessControlAllowCredentials().isEmpty());
        Assertions.assertTrue(configWithAccessControlMaxAge.exposedHeaders().isEmpty());
        var fullConfig = (CORSConfig) new CORS.Builder(config)
                .accessControlMaxAge(Duration.ofDays(3))
                .accessControlAllowCredentials(true)
                .header("Header")
                .exposedHeader("Exposed Header")
                .origin("Origin")
                .method("Method")
                .build();
        Assertions.assertEquals(3, fullConfig.accessControlMaxAge().map(Duration::toDays).orElse(null));
        Assertions.assertTrue(fullConfig.accessControlAllowCredentials().orElse(false));
        assertThat(fullConfig.methods()).isPresent().contains(List.of("Method"));
        assertThat(fullConfig.headers()).isPresent().contains(List.of("Header"));
        assertThat(fullConfig.exposedHeaders()).isPresent().contains(List.of("Exposed Header"));
        assertThat(fullConfig.origins()).isPresent().contains(List.of("Origin"));
    }
}
