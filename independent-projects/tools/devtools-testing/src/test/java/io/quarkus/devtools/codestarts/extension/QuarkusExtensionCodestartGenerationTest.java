package io.quarkus.devtools.codestarts.extension;

import static io.quarkus.devtools.testing.SnapshotTesting.assertThatDirectoryTreeMatchSnapshots;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import io.quarkus.devtools.codestarts.extension.QuarkusExtensionCodestartCatalog.QuarkusExtensionData;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.testing.SnapshotTesting;

class QuarkusExtensionCodestartGenerationTest {

    private static final Path testDirPath = Paths.get("target/extension-codestart-gen-test");

    @BeforeAll
    static void setUp() throws IOException {
        SnapshotTesting.deleteTestDirectory(testDirPath.toFile());
    }

    private QuarkusExtensionCodestartProjectInputBuilder prepareInput() {
        return QuarkusExtensionCodestartProjectInput.builder()
                .putData(QuarkusExtensionData.GROUP_ID, "org.extension")
                .putData(QuarkusExtensionData.NAMESPACE_ID, "quarkus-")
                .putData(QuarkusExtensionData.NAMESPACE_NAME, "Quarkus -")
                .putData(QuarkusExtensionData.EXTENSION_ID, "my-extension")
                .putData(QuarkusExtensionData.EXTENSION_NAME, "My Extension")
                .putData(QuarkusExtensionData.VERSION, "1.0.0-SNAPSHOT")
                .putData(QuarkusExtensionData.PACKAGE_NAME, "org.extension")
                .putData(QuarkusExtensionData.CLASS_NAME_BASE, "MyExtension")
                .putData(QuarkusExtensionData.JAVA_VERSION, "11");
    }

    @Test
    void generateDefaultProject(TestInfo testInfo) throws Throwable {
        final QuarkusExtensionCodestartProjectInput input = prepareInput()
                .build();
        final Path projectDir = testDirPath.resolve("default");
        getCatalog().createProject(input).generate(projectDir);
        assertThatDirectoryTreeMatchSnapshots(testInfo, projectDir);

        // External extensions (default layout) must declare <extensions>true</extensions>
        // on the quarkus-maven-plugin to avoid the Maven warning about missing extensions.
        // See https://github.com/quarkusio/quarkus/issues/53533
        final String itPom = Files.readString(projectDir.resolve("integration-tests/pom.xml"));
        assertThat(itPom).contains("<extensions>true</extensions>");
    }

    @Test
    void generateInQuarkusCoreProject(TestInfo testInfo) throws Throwable {
        final QuarkusExtensionCodestartProjectInput input = prepareInput()
                .putData(QuarkusExtensionData.IN_QUARKUS_CORE, true)
                .build();
        final Path projectDir = testDirPath.resolve("in-quarkus-core");
        getCatalog().createProject(input).generate(projectDir);

        // Extensions inside the Quarkus core repo must NOT declare <extensions>true</extensions>:
        // the quarkus-maven-plugin is built in the same reactor and is not yet available
        // as a Maven extension during the first build.
        // See https://github.com/quarkusio/quarkus/issues/53533
        final String itPom = Files.readString(projectDir.resolve("integration-tests/pom.xml"));
        assertThat(itPom).doesNotContain("<extensions>true</extensions>");
    }

    @Test
    void generateProjectWithoutTests(TestInfo testInfo) throws Throwable {
        final QuarkusExtensionCodestartProjectInput input = prepareInput()
                .withoutDevModeTest(true)
                .withoutIntegrationTests(true)
                .withoutUnitTest(true)
                .build();
        final Path projectDir = testDirPath.resolve("without-tests");
        getCatalog().createProject(input).generate(projectDir);
        assertThatDirectoryTreeMatchSnapshots(testInfo, projectDir);
    }

    private QuarkusExtensionCodestartCatalog getCatalog() throws IOException {
        return QuarkusExtensionCodestartCatalog.fromBaseCodestartsResources(MessageWriter.info());
    }

}
