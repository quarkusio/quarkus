package io.quarkus.it.panache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(PageItem.class, TestResources.class, NoPagingTestEndpoint.class))
            .setApplicationName("ddl-generation")
            .setApplicationVersion(Version.getVersion())
            .setRun(true)
            .setLogFileName("ddl-generation-test.log")
            .withConfigurationResource("ddlgeneration.properties");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void test() throws IOException {
        RestAssured.when().get("/no-paging-test").then().body(is("OK"));

        Path createSqlPath = prodModeTestResults.getBuildDir().resolve("quarkus-app/create.sql");
        assertThat(createSqlPath.toFile()).exists();
        assertThat(new String(Files.readAllBytes(createSqlPath))).contains("\n/");

        Path dropSqlPath = prodModeTestResults.getBuildDir().resolve("quarkus-app/drop.sql");
        assertThat(dropSqlPath.toFile()).exists();
        assertThat(new String(Files.readAllBytes(dropSqlPath))).contains("\n/");
    }

}
