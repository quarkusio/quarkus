package io.quarkus.devtools.codestarts.extension;

import static io.quarkus.devtools.testing.SnapshotTesting.assertThatDirectoryTreeMatchSnapshots;

import io.quarkus.devtools.codestarts.extension.QuarkusExtensionCodestartCatalog.QuarkusExtensionData;
import io.quarkus.devtools.testing.SnapshotTesting;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

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
                .putData(QuarkusExtensionData.CLASS_NAME_BASE, "MyExtension");
    }

    @Test
    void generateDefaultProject(TestInfo testInfo) throws Throwable {
        final QuarkusExtensionCodestartProjectInput input = prepareInput()
                .build();
        final Path projectDir = testDirPath.resolve("default");
        getCatalog().createProject(input).generate(projectDir);
        assertThatDirectoryTreeMatchSnapshots(testInfo, projectDir);
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
        return QuarkusExtensionCodestartCatalog.fromBaseCodestartsResources();
    }

}
