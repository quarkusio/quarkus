package io.quarkus.opentelemetry.runtime.tracing.restclient;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HttpUrlFilterTest {
    @Test
    void testUrl() {
        ClientTracingFilter filter = new ClientTracingFilter(null);

        String url = "https://quarkus.io";
        String result = filter.filterUserInfo(url);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(url, result);
    }

    @Test
    void testUrlWithPort() {
        ClientTracingFilter filter = new ClientTracingFilter(null);

        String url = "https://quarkus.io:9091";
        String result = filter.filterUserInfo(url);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(url, result);
    }

    @Test
    void testUrlWithPath() {
        ClientTracingFilter filter = new ClientTracingFilter(null);

        String url = "https://quarkus.io/guides/";
        String result = filter.filterUserInfo(url);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(url, result);
    }

    @Test
    void testUserWithEmptyPassword() {
        ClientTracingFilter filter = new ClientTracingFilter(null);

        String url = "https://username:@quarkus.io";
        String result = filter.filterUserInfo(url);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("https://quarkus.io", result);
    }

    @Test
    void testUserWithEmptyPasswordAndPort() {
        ClientTracingFilter filter = new ClientTracingFilter(null);

        String url = "https://username:@quarkus.io:9212";
        String result = filter.filterUserInfo(url);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("https://quarkus.io:9212", result);
    }

    @Test
    void testUserWithPassword() {
        ClientTracingFilter filter = new ClientTracingFilter(null);

        String url = "https://username:password@quarkus.io";
        String result = filter.filterUserInfo(url);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("https://quarkus.io", result);
    }

    @Test
    void testUserWithPasswordAndPort() {
        ClientTracingFilter filter = new ClientTracingFilter(null);

        String url = "https://username:password@quarkus.io:9000";
        String result = filter.filterUserInfo(url);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("https://quarkus.io:9000", result);
    }
}
