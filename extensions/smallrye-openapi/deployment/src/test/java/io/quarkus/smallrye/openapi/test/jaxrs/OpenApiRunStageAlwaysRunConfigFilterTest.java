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
import java.nio.file.Files;
import java.nio.file.Paths;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.openapi.OpenApiFilter;
import io.quarkus.smallrye.openapi.OpenApiFilter.RunStage;
import io.quarkus.smallrye.openapi.runtime.OpenApiConstants;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @deprecated This is basically a copy from OpenApiRunStageFilterTest, which is okay since only the special interactions with
 *             quarkus.smallrye-openapi.always-run-filter=true are tested additionally.
 *             That config property is deprecated as well. See
 *             io.quarkus.smallrye.openapi.runtime.OpenApiDocumentService#prepareDocument for a reference of the expected effect
 *             of always-run-filter
 */
@Deprecated(since = "3.32", forRemoval = true)
class OpenApiRunStageAlwaysRunConfigFilterTest {

    private static final String STORE_SCHEMA_DIRECTORY = "target/generated/OpenApiRunStageAlwaysRunConfigFilterTest/";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(BuildFilter.class)
                    .addClass(RuntimeStartupFilter.class)
                    .addClass(RuntimePerRequestFilter.class)
                    .addClass(RunFilter.class)
                    .addClass(BothFilter.class)
                    .addAsResource(
                            new StringAsset("""
                                    quarkus.smallrye-openapi.store-schema-directory=%s
                                    quarkus.smallrye-openapi.always-run-filter=true
                                    """.formatted(STORE_SCHEMA_DIRECTORY)),
                            "application.properties"));

    abstract static class BaseFilter implements OASFilter {
        protected static final String EXTENSION_NAME_PREFIX = "x-global-invocation-count";

        // If the filter run at runtime_startup or runtime_request, this will be greater than 0.
        // Executions during Build will increase this counter, but in a different class loader.
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
    @OpenApiFilter(stages = RunStage.BUILD)
    public static class BuildFilter extends BaseFilter implements OASFilter {
        public static final String EXTENSION_NAME = EXTENSION_NAME_PREFIX + "-build";

        public String getExtensionName() {
            return EXTENSION_NAME;
        }
    }

    @Singleton
    @OpenApiFilter(stages = RunStage.RUNTIME_STARTUP)
    public static class RuntimeStartupFilter extends BaseFilter implements OASFilter {
        public static final String EXTENSION_NAME = EXTENSION_NAME_PREFIX + "-runtime-startup";

        public String getExtensionName() {
            return EXTENSION_NAME;
        }
    }

    @Singleton
    @OpenApiFilter(stages = RunStage.RUNTIME_PER_REQUEST)
    public static class RuntimePerRequestFilter extends BaseFilter implements OASFilter {
        public static final String EXTENSION_NAME = EXTENSION_NAME_PREFIX + "-runtime-per-request";

        public String getExtensionName() {
            return EXTENSION_NAME;
        }
    }

    @Singleton
    @OpenApiFilter(stages = RunStage.RUN)
    public static class RunFilter extends BaseFilter implements OASFilter {
        public static final String EXTENSION_NAME = EXTENSION_NAME_PREFIX + "-run";

        public String getExtensionName() {
            return EXTENSION_NAME;
        }
    }

    @Singleton
    @OpenApiFilter(stages = RunStage.BOTH)
    public static class BothFilter extends BaseFilter implements OASFilter {
        public static final String EXTENSION_NAME = EXTENSION_NAME_PREFIX + "-both";

        public String getExtensionName() {
            return EXTENSION_NAME;
        }
    }

    @Inject
    OpenApiRunStageAlwaysRunConfigFilterTest.BuildFilter buildFilter;

    @Inject
    OpenApiRunStageAlwaysRunConfigFilterTest.RuntimeStartupFilter runtimeStartupFilter;

    @Inject
    OpenApiRunStageAlwaysRunConfigFilterTest.RuntimePerRequestFilter runtimePerRequestFilter;

    @Inject
    OpenApiRunStageAlwaysRunConfigFilterTest.RunFilter runFilter;

    @Inject
    OpenApiRunStageAlwaysRunConfigFilterTest.BothFilter bothFilter;

    @Test
    public void testFiltersApplied() throws IOException {
        // remember:
        // - currentCLInvocationCount is only visible for runtime_startup and runtime_per_request executions
        // - changes to the openapi model are not always persisted for the next filter execution

        String buildTimeDocument = readBuildTimeDocument();

        // pattern to find an openapi extension with a specific value anywhere (no matter new lines) in the rendered openapi document
        String patternFormat = "(?s).*\"%s\" *: *%s.*";
        // verify which filters where run during BUILD stage
        assertThat(buildTimeDocument, matchesRegex(patternFormat.formatted(buildFilter.getExtensionName(), 1)));
        assertThat(buildTimeDocument, not(containsString(runtimeStartupFilter.getExtensionName())));
        assertThat(buildTimeDocument, not(containsString(runtimePerRequestFilter.getExtensionName())));
        assertThat(buildTimeDocument, not(containsString(runFilter.getExtensionName())));
        assertThat(buildTimeDocument, matchesRegex(patternFormat.formatted(bothFilter.getExtensionName(), 1)));

        // Verify which filters run during SmallRyeOpenApiProcessor.applyRuntimeFilters
        // should just be the build stage + runtime startup stage
        String storedDocument = Files.readString(Paths.get(STORE_SCHEMA_DIRECTORY, "openapi.json"));
        assertThat(storedDocument, matchesRegex(patternFormat.formatted(buildFilter.getExtensionName(), 1)));
        assertThat(storedDocument, matchesRegex(patternFormat.formatted(runtimeStartupFilter.getExtensionName(), 1)));
        assertThat(storedDocument, not(containsString(runtimePerRequestFilter.getExtensionName())));
        assertThat(storedDocument, matchesRegex(patternFormat.formatted(runFilter.getExtensionName(), 1)));
        assertThat(storedDocument, matchesRegex(patternFormat.formatted(bothFilter.getExtensionName(), 2)));

        // verify which filters where run during RUNTIME_STARTUP stage
        assertThat(buildFilter.currentCLInvocationCount, is(0));
        assertThat(runtimeStartupFilter.currentCLInvocationCount, is(1));
        assertThat(runtimePerRequestFilter.currentCLInvocationCount, is(0));
        assertThat(runFilter.currentCLInvocationCount, is(0));
        assertThat(bothFilter.currentCLInvocationCount, is(0));

        // now we verify which filters are run at runtime request
        // results should be build stage + run stage + runtime request stage
        given()
                .when().get("/q/openapi.json")
                .then()
                .statusCode(200)
                .body(buildFilter.getExtensionName(), equalTo(1))
                .body(runtimeStartupFilter.getExtensionName(), equalTo(1))
                .body(runtimePerRequestFilter.getExtensionName(), equalTo(1))
                .body(runFilter.getExtensionName(), equalTo(1))
                .body(bothFilter.getExtensionName(), equalTo(2));
        assertThat(buildFilter.currentCLInvocationCount, is(0));
        assertThat(runtimeStartupFilter.currentCLInvocationCount, is(1));
        assertThat(runtimePerRequestFilter.currentCLInvocationCount, is(1));
        assertThat(runFilter.currentCLInvocationCount, is(1));
        assertThat(bothFilter.currentCLInvocationCount, is(1));

        // Verify that only the runtime request filters are run again on another request
        // however, changes from previous run runtime request filters are not persisted, i.e. invocation count is still 1 in the extension, but in the var will be 2
        given()
                .when().get("/q/openapi.json")
                .then()
                .statusCode(200)
                .body(buildFilter.getExtensionName(), equalTo(1))
                .body(runtimeStartupFilter.getExtensionName(), equalTo(1))
                .body(runtimePerRequestFilter.getExtensionName(), equalTo(1))
                .body(runFilter.getExtensionName(), equalTo(1))
                .body(bothFilter.getExtensionName(), equalTo(2));
        assertThat(buildFilter.currentCLInvocationCount, is(0));
        assertThat(runtimeStartupFilter.currentCLInvocationCount, is(1));
        assertThat(runtimePerRequestFilter.currentCLInvocationCount, is(2));
        assertThat(runFilter.currentCLInvocationCount, is(2));
        assertThat(bothFilter.currentCLInvocationCount, is(2));
    }

    private String readBuildTimeDocument() throws IOException {

        return new String(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(OpenApiConstants.BASE_NAME + ".JSON").readAllBytes(), StandardCharsets.UTF_8);
    }
}
