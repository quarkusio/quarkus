package io.quarkus.it.main;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.it.main.TestHTTPResourceRootPathTest.RootPathTestProfile;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTestExtension;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@TestProfile(RootPathTestProfile.class)
public class TestHTTPResourceRootPathTest {
    @RegisterExtension
    static QuarkusTestExtension quarkusTestExtension = new QuarkusTestExtension();

    @TestHTTPResource
    URI uri;

    @Test
    void rootPath() {
        assertTrue(uri.toString().contains("/foo"));
    }

    public static class RootPathTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.http.root-path", "foo");
        }
    }
}
