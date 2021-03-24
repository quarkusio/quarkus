package io.quarkus.it.legacy.redirect;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class LegacyRedirectTest {

    // Default configuration

    @TestHTTPResource(value = "/")
    URL hostPortUrl;

    WebClientUtil clientUtil = new WebClientUtil();

    @BeforeEach
    void setUrl() {
        clientUtil.setHostPortUrl(hostPortUrl);
    }

    @Test
    public void testGraphQlUnchangedNeverRedirect() {
        // Not the same as 404.. graphql is found in the right place
        clientUtil.validate("/graphql", 405);
    }

    @Test
    public void testGraphQlUiWithRedirect() {
        clientUtil.validate("/graphql-ui", 301, "/q/graphql-ui");
        clientUtil.validate("/q/graphql-ui", 302);
    }

    @Test
    // TODO - MP4 - Require SR Health 3.0.1
    @Disabled
    public void testHealthWithRedirect() {
        clientUtil.validate("/health", 301, "/q/health");
        clientUtil.validate("/q/health", 200);

        clientUtil.validate("/health/group", 301, "/q/health/group");
        clientUtil.validate("/q/health/group", 200);

        clientUtil.validate("/health/live", 301, "/q/health/live");
        clientUtil.validate("/q/health/live", 200);

        clientUtil.validate("/health/ready", 301, "/q/health/ready");
        clientUtil.validate("/q/health/ready", 200);

        clientUtil.validate("/health/well", 301, "/q/health/well");
        clientUtil.validate("/q/health/well", 200);

        clientUtil.validate("/health-ui", 301, "/q/health-ui");
        clientUtil.validate("/q/health-ui", 302);
    }

    @Test
    public void testMetricsWithRedirect() {
        clientUtil.validate("/metrics", 301, "/q/metrics");
        clientUtil.validateText("/q/metrics", 200);
    }

    @Test
    public void testOpenApiWithRedirect() {
        clientUtil.validate("/openapi", 301, "/q/openapi");
        clientUtil.validate("/openapi?format=JSON", 301, "/q/openapi?format=JSON");
        clientUtil.validateContentType("/q/openapi?format=JSON", 200, "application/json");
        clientUtil.followForContentType("/openapi?format=JSON", 200, "application/json");
    }

    @Test
    public void testSwaggerUiWithRedirect() {
        clientUtil.validate("/swagger-ui", 301, "/q/swagger-ui");
        clientUtil.validate("/q/swagger-ui", 302);
    }
}
