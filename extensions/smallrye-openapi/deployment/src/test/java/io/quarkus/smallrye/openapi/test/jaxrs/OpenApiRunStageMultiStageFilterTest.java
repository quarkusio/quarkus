package io.quarkus.smallrye.openapi.test.jaxrs;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.openapi.OpenApiFilter;
import io.quarkus.smallrye.openapi.OpenApiFilter.RunStage;
import io.quarkus.smallrye.openapi.runtime.OpenApiConstants;
import io.quarkus.test.QuarkusUnitTest;

class OpenApiRunStageMultiStageFilterTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(BuildAndPerRequestFilter.class)
                    .addClass(StartupAndPerRequestFilter.class));

    abstract static class BaseFilter implements OASFilter {
        protected static final String EXTENSION_NAME_PREFIX = "x-multi-stage-invocation-count";

        public int currentCLInvocationCount;

        public abstract String getExtensionName();

        @Override
        public void filterOpenAPI(OpenAPI openAPI) {
            currentCLInvocationCount++;

            int invocationCount = 0;
            if (openAPI.hasExtension(getExtensionName())) {
                invocationCount = (int) openAPI.getExtension(getExtensionName());
            }
            invocationCount++;

            openAPI.addExtension(getExtensionName(), invocationCount);
        }
    }

    @Singleton
    @OpenApiFilter(stages = { RunStage.BUILD, RunStage.RUNTIME_PER_REQUEST })
    public static class BuildAndPerRequestFilter extends BaseFilter implements OASFilter {
        public static final String EXTENSION_NAME = EXTENSION_NAME_PREFIX + "-build-and-per-request";

        public String getExtensionName() {
            return EXTENSION_NAME;
        }
    }

    @Singleton
    @OpenApiFilter(stages = { RunStage.RUNTIME_STARTUP, RunStage.RUNTIME_PER_REQUEST })
    public static class StartupAndPerRequestFilter extends BaseFilter implements OASFilter {
        public static final String EXTENSION_NAME = EXTENSION_NAME_PREFIX + "-startup-and-per-request";

        public String getExtensionName() {
            return EXTENSION_NAME;
        }
    }

    @Inject
    BuildAndPerRequestFilter buildAndPerRequestFilter;

    @Inject
    StartupAndPerRequestFilter startupAndPerRequestFilter;

    @Test
    public void testMultiStageFiltersApplied() throws IOException {
        String patternFormat = "(?s).*\"%s\" *: *%s.*";

        // Verify which filters have run at build time
        String buildTimeDocument = readBuildTimeDocument();
        assertThat(buildTimeDocument,
                matchesRegex(patternFormat.formatted(BuildAndPerRequestFilter.EXTENSION_NAME, 1)));
        assertThat(buildTimeDocument, not(containsString(StartupAndPerRequestFilter.EXTENSION_NAME)));

        // Verify which filters have run during runtime startup
        assertThat(buildAndPerRequestFilter.currentCLInvocationCount, is(0));
        assertThat(startupAndPerRequestFilter.currentCLInvocationCount, is(1));

        given()
                .when().get("/q/openapi.json")
                .then()
                .statusCode(200)
                .body(BuildAndPerRequestFilter.EXTENSION_NAME, equalTo(2))
                .body(StartupAndPerRequestFilter.EXTENSION_NAME, equalTo(2));
        assertThat(buildAndPerRequestFilter.currentCLInvocationCount, is(1));
        assertThat(startupAndPerRequestFilter.currentCLInvocationCount, is(2));

        // stays the same on 2, as the openapi document is not persisted after request filters run
        given()
                .when().get("/q/openapi.json")
                .then()
                .statusCode(200)
                .body(BuildAndPerRequestFilter.EXTENSION_NAME, equalTo(2))
                .body(StartupAndPerRequestFilter.EXTENSION_NAME, equalTo(2));
        // but the currentCLInvocationCount gets increased
        assertThat(buildAndPerRequestFilter.currentCLInvocationCount, is(2));
        assertThat(startupAndPerRequestFilter.currentCLInvocationCount, is(3));
    }

    private String readBuildTimeDocument() throws IOException {
        return new String(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(OpenApiConstants.BASE_NAME + ".JSON").readAllBytes(), StandardCharsets.UTF_8);
    }
}
