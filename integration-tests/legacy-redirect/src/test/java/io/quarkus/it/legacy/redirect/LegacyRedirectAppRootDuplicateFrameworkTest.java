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
@TestProfile(LegacyRedirectAppRootDuplicateFrameworkTest.SetApplicationSameFrameworkRoot.class)
class LegacyRedirectAppRootDuplicateFrameworkTest {

    // When the root path and the framework path are the same, there are
    // no redirects at all.

    public static class SetApplicationSameFrameworkRoot implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> overrides = new HashMap<>();
            overrides.put("quarkus.http.root-path", "/app");
            overrides.put("quarkus.http.non-application-root-path", "/app");
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

        clientUtil.validate("/app/graphql-ui", 302);
    }

    @Test
    // TODO - MP4 - Require SR Health 3.0.1
    @Disabled
    public void testHealthWithRedirect() {
        // Not found: moved with application endpoint
        clientUtil.validate("/health", 404);

        clientUtil.validate("/app/health", 200);
        clientUtil.validate("/app/health/group", 200);
        clientUtil.validate("/app/health/live", 200);
        clientUtil.validate("/app/health/ready", 200);
        clientUtil.validate("/app/health/well", 200);
        clientUtil.validate("/app/health-ui", 302);
    }

    @Test
    public void testMetricsWithRedirect() {
        // Not found: moved with application endpoint
        clientUtil.validate("/metrics", 404);

        clientUtil.validateText("/app/metrics", 200);
    }

    @Test
    public void testOpenApiWithRedirect() {
        // Not found: moved with application endpoint
        clientUtil.validate("/openapi", 404);

        clientUtil.validate("/app/openapi", 200);
    }

    @Test
    public void testSwaggerUiWithRedirect() {
        // Not found: moved with application endpoint
        clientUtil.validate("/swagger-ui", 404);

        clientUtil.validate("/app/swagger-ui", 302);
    }
}
