package io.quarkus.it.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.net.URL;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(RandomTestPortTestCase.TestPortProfile.class)
public class RandomTestPortTestCase {

    @ConfigProperty(name = "quarkus.http.port")
    int httpPort;

    @ConfigProperty(name = "quarkus.http.test-port")
    int httpTestPort;

    @TestHTTPResource(value = "/some-path")
    URL httpTestUrl;

    @ConfigProperty(name = "quarkus.http.ssl-port")
    int httpsPort;

    @ConfigProperty(name = "quarkus.http.test-ssl-port")
    int httpsTestPort;

    @TestHTTPResource(value = "/some-path", ssl = true)
    URL httpsTestUrl;

    @Test
    public void testHttpTestPort() {
        assertNotEquals(0, httpTestPort);
        assertNotEquals(8080, httpTestPort);
        assertNotEquals(8081, httpTestPort);

        assertEquals(httpTestPort, httpPort);
        assertEquals(httpTestPort, httpTestUrl.getPort());
    }

    @Test
    public void testHttpsTestPort() {
        assertNotEquals(0, httpsTestPort);
        assertNotEquals(8443, httpsTestPort);
        assertNotEquals(8444, httpsTestPort);

        assertEquals(httpsTestPort, httpsPort);
        assertEquals(httpsTestPort, httpsTestUrl.getPort());
    }

    @Test
    public void testLegacyProperties() {
        Config config = ConfigProvider.getConfig();

        testLegacyProperty(config, "quarkus.http.ssl-port", "quarkus.https.port", httpsPort);
        testLegacyProperty(config, "quarkus.http.test-ssl-port", "quarkus.https.test-port", httpsTestPort);
        testLegacyProperty(config, "%test.quarkus.http.ssl-port", "%test.quarkus.https.port", httpsTestPort);
    }

    private void testLegacyProperty(Config config, String correctName, String legacyName, int expectedValue) {
        Optional<Integer> portWithCorrectProperty = config.getOptionalValue(correctName, Integer.class);
        Optional<Integer> portWithLegacyProperty = config.getOptionalValue(legacyName, Integer.class);

        assertEquals(Optional.of(expectedValue), portWithCorrectProperty);
        assertEquals(portWithCorrectProperty, portWithLegacyProperty);
    }

    public static class TestPortProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.http.test-port", "0", "quarkus.http.test-ssl-port", "0");
        }
    }
}
