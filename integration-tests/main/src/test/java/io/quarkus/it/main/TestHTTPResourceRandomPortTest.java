package io.quarkus.it.main;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.it.main.TestHTTPResourceRandomPortTest.RandomPortTestProfile;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTestExtension;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@TestProfile(RandomPortTestProfile.class)
class TestHTTPResourceRandomPortTest {
    @RegisterExtension
    static QuarkusTestExtension quarkusTestExtension = new QuarkusTestExtension();

    @TestHTTPResource
    URI uri;

    @Test
    void randomPort() {
        assertTrue(uri.getPort() > 0);
        assertTrue(uri.getPort() != 8080);
        assertTrue(uri.getPort() != 8081);
    }

    public static class RandomPortTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.http.test-port", "0");
        }
    }
}
