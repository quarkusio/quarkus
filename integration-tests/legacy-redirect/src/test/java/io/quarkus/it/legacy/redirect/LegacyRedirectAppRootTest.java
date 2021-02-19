package io.quarkus.it.legacy.redirect;

import java.net.URL;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(LegacyRedirectAppRootTest.SetApplicationRoot.class)
class LegacyRedirectAppRootTest {

    // Legacy redirect behavior is implicitly scoped to the configured
    // http root (i.e. /app/health will be redirected)

    public static class SetApplicationRoot implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Collections.singletonMap("quarkus.http.root-path", "/app");
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

        clientUtil.validate("/app/graphql-ui", 301, "/app/q/graphql-ui");
        clientUtil.validate("/app/q/graphql-ui", 302);
    }

    @Test
    public void testHealthWithRedirect() {
        // Not found: moved with application endpoint
        clientUtil.validate("/health", 404);

        clientUtil.validate("/app/health", 301, "/app/q/health");
        clientUtil.validate("/app/q/health", 200);

        clientUtil.validate("/app/health/group", 301, "/app/q/health/group");
        clientUtil.validate("/app/q/health/group", 200);

        clientUtil.validate("/app/health/live", 301, "/app/q/health/live");
        clientUtil.validate("/app/q/health/live", 200);

        clientUtil.validate("/app/health/ready", 301, "/app/q/health/ready");
        clientUtil.validate("/app/q/health/ready", 200);

        clientUtil.validate("/app/health/well", 301, "/app/q/health/well");
        clientUtil.validate("/app/q/health/well", 200);

        clientUtil.validate("/app/health-ui", 301, "/app/q/health-ui");
        clientUtil.validate("/app/q/health-ui", 302);
    }

    @Test
    public void testMetricsWithRedirect() {
        // Not found: moved with application endpoint
        clientUtil.validate("/metrics", 404);

        clientUtil.validate("/app/metrics", 301, "/app/q/metrics");
        clientUtil.validateText("/app/q/metrics", 200);
    }

    @Test
    public void testOpenApiWithRedirect() {
        // Not found: moved with application endpoint
        clientUtil.validate("/openapi", 404);

        clientUtil.validate("/app/openapi", 301, "/app/q/openapi");
        clientUtil.validate("/app/q/openapi", 200);
    }

    @Test
    public void testSwaggerUiWithRedirect() {
        // Not found: moved with application endpoint
        clientUtil.validate("/swagger-ui", 404);

        clientUtil.validate("/app/swagger-ui", 301, "/app/q/swagger-ui");
        clientUtil.validate("/app/q/swagger-ui", 302);
    }
}
