package io.quarkus.it.panache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;
import io.restassured.RestAssured;

/**
 * Verifies that DDL scripts are generated when script generation is configured in application.properties.
 */
public class DDLGenerationPMT {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PageItem.class, TestResources.class, NoPagingTestEndpoint.class))
            .setApplicationName("ddl-generation")
            .setApplicationVersion(Version.getVersion())
            .setRun(true)
            .setLogFileName("ddl-generation-test.log")
            .withConfigurationResource("ddlgeneration.properties");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void test() {
        RestAssured.when().get("/no-paging-test").then().body(is("OK"));

        assertThat(prodModeTestResults.getBuildDir().resolve("quarkus-app/create.sql").toFile()).exists();
        assertThat(prodModeTestResults.getBuildDir().resolve("quarkus-app/drop.sql").toFile()).exists();
    }

}
