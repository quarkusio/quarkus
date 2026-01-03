package io.quarkus.it.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.net.URL;
import java.util.Map;

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

    @TestHTTPResource(value = "/some-path", tls = true)
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

    public static class TestPortProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.http.test-port", "0", "quarkus.http.test-ssl-port", "0");
        }
    }
}
