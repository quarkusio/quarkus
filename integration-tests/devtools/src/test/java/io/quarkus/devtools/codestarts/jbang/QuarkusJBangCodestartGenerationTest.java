package io.quarkus.devtools.codestarts.jbang;

import static io.quarkus.devtools.codestarts.jbang.QuarkusJBangCodestartCatalog.JBangDataKey.QUARKUS_BOM_ARTIFACT_ID;
import static io.quarkus.devtools.codestarts.jbang.QuarkusJBangCodestartCatalog.JBangDataKey.QUARKUS_BOM_GROUP_ID;
import static io.quarkus.devtools.codestarts.jbang.QuarkusJBangCodestartCatalog.JBangDataKey.QUARKUS_BOM_VERSION;
import static io.quarkus.devtools.testing.SnapshotTesting.assertThatDirectoryTreeMatchSnapshots;
import static io.quarkus.devtools.testing.SnapshotTesting.assertThatMatchSnapshot;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import io.quarkus.devtools.PlatformAwareTestBase;
import io.quarkus.devtools.testing.SnapshotTesting;

class QuarkusJBangCodestartGenerationTest extends PlatformAwareTestBase {

    private static final Path testDirPath = Paths.get("target/jbang-codestart-gen-test");

    @BeforeAll
    static void setUp() throws IOException {
        SnapshotTesting.deleteTestDirectory(testDirPath.toFile());
    }

    @Test
    void generateDefaultProject(TestInfo testInfo) throws Throwable {
        final QuarkusJBangCodestartProjectInput input = QuarkusJBangCodestartProjectInput.builder()
                .putData(QUARKUS_BOM_GROUP_ID, "io.quarkus")
                .putData(QUARKUS_BOM_ARTIFACT_ID, "quarkus-bom")
                .putData(QUARKUS_BOM_VERSION, "999-MOCK")
                .build();
        final Path projectDir = testDirPath.resolve("default");
        getCatalog().createProject(input).generate(projectDir);
        assertThatDirectoryTreeMatchSnapshots(testInfo, projectDir);
        assertThatMatchSnapshot(testInfo, projectDir, "src/GreetingResource.java");
    }

    @Test
    void generatePicocliProject(TestInfo testInfo) throws Throwable {
        final QuarkusJBangCodestartProjectInput input = QuarkusJBangCodestartProjectInput.builder()
                .addCodestart("jbang-picocli-code")
                .putData(QUARKUS_BOM_GROUP_ID, "io.quarkus")
                .putData(QUARKUS_BOM_ARTIFACT_ID, "quarkus-bom")
                .putData(QUARKUS_BOM_VERSION, "999-MOCK")
                .build();
        final Path projectDir = testDirPath.resolve("picocli");
        getCatalog().createProject(input).generate(projectDir);
        assertThatDirectoryTreeMatchSnapshots(testInfo, projectDir);
        assertThatMatchSnapshot(testInfo, projectDir, "src/GreetingCommand.java");
    }

    private QuarkusJBangCodestartCatalog getCatalog() throws IOException {
        return QuarkusJBangCodestartCatalog.fromResourceLoader(getCodestartsResourceLoader());
    }

}
