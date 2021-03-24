package io.quarkus.it.legacy.redirect;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(LegacyRedirectFrameworkRootTest.SetFrameworkRoot.class)
class LegacyRedirectFrameworkRootTest {

    // Legacy redirect behavior is implicitly scoped to the configured
    // http root (i.e. /app/health will be redirected)

    public static class SetFrameworkRoot implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> overrides = new HashMap<>();
            overrides.put("quarkus.http.root-path", "/app");
            overrides.put("quarkus.http.non-application-root-path", "/framework");
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
    public void testGraphQlUnchangedNeverRedirect() {
        // Not found: moved with application endpoint
        clientUtil.validate("/graphql", 404);
        // Not the same as 404.. graphql is found in the right place
        clientUtil.validate("/app/graphql", 405);
    }

    @Test
    public void testGraphQlUiWithRedirect() {
        // Not found: moved with application endpoint
        clientUtil.validate("/graphql-ui", 404);

        clientUtil.validate("/app/graphql-ui", 301, "/framework/graphql-ui");
        clientUtil.validate("/framework/graphql-ui", 302);
    }

    @Test
    // TODO - MP4 - Require SR Health 3.0.1
    @Disabled
    public void testHealthWithRedirect() {
        // Not found: moved with application endpoint
        clientUtil.validate("/health", 404);

        clientUtil.validate("/app/health", 301, "/framework/health");
        clientUtil.validate("/framework/health", 200);

        clientUtil.validate("/app/health/group", 301, "/framework/health/group");
        clientUtil.validate("/framework/health/group", 200);

        clientUtil.validate("/app/health/live", 301, "/framework/health/live");
        clientUtil.validate("/framework/health/live", 200);

        clientUtil.validate("/app/health/ready", 301, "/framework/health/ready");
        clientUtil.validate("/framework/health/ready", 200);

        clientUtil.validate("/app/health/well", 301, "/framework/health/well");
        clientUtil.validate("/framework/health/well", 200);

        clientUtil.validate("/app/health-ui", 301, "/framework/health-ui");
        clientUtil.validate("/framework/health-ui", 302);
    }

    @Test
    public void testMetricsWithRedirect() {
        // Not found: moved with application endpoint
        clientUtil.validate("/metrics", 404);

        clientUtil.validate("/app/metrics", 301, "/framework/metrics");
        clientUtil.validateText("/framework/metrics", 200);
    }

    @Test
    public void testOpenApiWithRedirect() {
        // Not found: moved with application endpoint
        clientUtil.validate("/openapi", 404);

        clientUtil.validate("/app/openapi", 301, "/framework/openapi");
        clientUtil.validate("/framework/openapi", 200);
    }

    @Test
    public void testSwaggerUiWithRedirect() {
        // Not found: moved with application endpoint
        clientUtil.validate("/swagger-ui", 404);

        clientUtil.validate("/app/swagger-ui", 301, "/framework/swagger-ui");
        clientUtil.validate("/framework/swagger-ui", 302);
    }
}
