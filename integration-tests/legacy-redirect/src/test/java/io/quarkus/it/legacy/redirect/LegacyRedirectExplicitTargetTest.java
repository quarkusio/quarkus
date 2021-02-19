package io.quarkus.it.legacy.redirect;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(LegacyRedirectExplicitTargetTest.SetExplicitTarget.class)
class LegacyRedirectExplicitTargetTest {

    // When explicit endpoints are specified, those endpoints should be used
    // directly, which should disable the compatibility redirect for that
    // endpoint (404)

    public static class SetExplicitTarget implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> overrides = new HashMap<>();
            overrides.put("quarkus.http.root-path", "/app");
            overrides.put("quarkus.http.non-application-root-path", "/framework");
            overrides.put("quarkus.smallrye-metrics.path", "/my-metrics");
            overrides.put("quarkus.smallrye-health.liveness-path", "/liveness");
            return overrides;
        }
    }

    @TestHTTPResource(value = "/")
    URL hostPortUrl;

    WebClientUtil clientUtil = new WebClientUtil();

    @BeforeEach
    void setUrl() {
        clientUtil.setHostPortUrl(hostPortUrl);
    }

    @Test
    public void testHealthWithRedirect() {
        // Not found: moved with application endpoint
        clientUtil.validate("/health", 404);

        clientUtil.validate("/app/health", 301, "/framework/health");
        clientUtil.validate("/framework/health", 200);

        clientUtil.validate("/app/health/liveness", 404);
        clientUtil.validate("/framework/health/liveness", 404);
        clientUtil.validate("/liveness", 200);
    }

    @Test
    public void testMetricsWithRedirect() {
        // Not found: moved with application endpoint
        clientUtil.validate("/metrics", 404);
        clientUtil.validate("/app/metrics", 404);
        clientUtil.validate("/framework/metrics", 404);

        clientUtil.validateText("/my-metrics", 200);
        clientUtil.validate("/app/my-metrics", 404);
        clientUtil.validate("/framework/my-metrics", 404);
    }
}
