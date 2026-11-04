package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.client.HttpResponse;

/**
 * The default 429 body is RFC 9457 JSON only when the client accepts a JSON type, and plain text in every other case,
 * including no {@code Accept} header and {@code *}{@code /*}. It never names the rule, which is internal configuration.
 */
public class RejectionResponseTest {

    private static final String RULE = "internal-login-burst";
    private static final String PREFIX = "quarkus.http.rate-limit." + RULE + ".";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class))
            .overrideConfigKey(PREFIX + "paths", "/*")
            .overrideConfigKey(PREFIX + "phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "request-key", "header:X-Client")
            .overrideRuntimeConfigKey(PREFIX + "permits", "1")
            .overrideRuntimeConfigKey(PREFIX + "window", "1h");

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    TestClient http;

    @BeforeEach
    void createClient() {
        http = new TestClient(vertx, url);
    }

    @AfterEach
    void closeClient() {
        http.close();
    }

    @Test
    void textWithoutAnAcceptHeader() {
        assertText(reject(Map.of()));
    }

    @Test
    void textWhenAnyTypeIsAccepted() {
        assertText(reject(Map.of("Accept", "*/*")));
    }

    @Test
    void textForABrowser() {
        assertText(reject(Map.of("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")));
    }

    @Test
    void textWhenTextIsPreferred() {
        assertText(reject(Map.of("Accept", "text/plain, application/json;q=0.5")));
    }

    @Test
    void problemJsonWhenAccepted() {
        HttpResponse<Buffer> rejected = reject(Map.of("Accept", "application/problem+json"));
        assertThat(contentType(rejected)).isEqualTo("application/problem+json");
        assertThat(rejected.bodyAsString()).contains("\"status\":429").contains("\"title\":\"Too Many Requests\"");
        assertNoRule(rejected);
    }

    @Test
    void plainJsonWhenOnlyJsonIsAccepted() {
        HttpResponse<Buffer> rejected = reject(Map.of("Accept", "application/json"));
        assertThat(contentType(rejected)).isEqualTo("application/json");
        assertThat(rejected.bodyAsString()).contains("\"status\":429");
        assertNoRule(rejected);
    }

    @Test
    void jsonWhenJsonIsPreferredOverAnyType() {
        HttpResponse<Buffer> rejected = reject(Map.of("Accept", "application/json, */*;q=0.1"));
        assertThat(contentType(rejected)).isEqualTo("application/json");
        assertNoRule(rejected);
    }

    private HttpResponse<Buffer> reject(Map<String, String> headers) {
        String client = UUID.randomUUID().toString();
        // the resource produces text, so only the rejected request carries the headers under test
        assertThat(http.get("/x", Map.of("X-Client", client)).statusCode()).isEqualTo(200);
        Map<String, String> all = new HashMap<>(headers);
        all.put("X-Client", client);
        HttpResponse<Buffer> rejected = http.get("/x", all);
        assertThat(rejected.statusCode()).isEqualTo(429);
        assertThat(rejected.getHeader(HttpHeaders.RETRY_AFTER.toString())).isNotNull();
        return rejected;
    }

    private static String contentType(HttpResponse<Buffer> response) {
        return response.getHeader(HttpHeaders.CONTENT_TYPE.toString());
    }

    private static void assertText(HttpResponse<Buffer> rejected) {
        assertThat(contentType(rejected)).isEqualTo("text/plain; charset=utf-8");
        assertThat(rejected.bodyAsString()).isEqualTo("Too Many Requests");
    }

    private static void assertNoRule(HttpResponse<Buffer> rejected) {
        assertThat(rejected.bodyAsString()).doesNotContain(RULE).doesNotContain("rule");
    }
}
