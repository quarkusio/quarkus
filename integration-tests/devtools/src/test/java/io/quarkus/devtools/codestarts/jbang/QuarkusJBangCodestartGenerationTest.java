package io.quarkus.devtools.codestarts.jbang;

import static io.quarkus.devtools.codestarts.jbang.QuarkusJBangCodestartCatalog.DataKey.QUARKUS_BOM_ARTIFACT_ID;
import static io.quarkus.devtools.codestarts.jbang.QuarkusJBangCodestartCatalog.DataKey.QUARKUS_BOM_GROUP_ID;
import static io.quarkus.devtools.codestarts.jbang.QuarkusJBangCodestartCatalog.DataKey.QUARKUS_BOM_VERSION;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.PlatformAwareTestBase;
import io.quarkus.devtools.ProjectTestUtil;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;

class QuarkusJBangCodestartGenerationTest extends PlatformAwareTestBase {

    private static final Path testDirPath = Paths.get("target/jbang-codestart-gen-test");

    @BeforeAll
    static void setUp() throws IOException {
        ProjectTestUtil.delete(testDirPath.toFile());
    }

    @Test
    void generateDefaultProject() throws IOException {
        final QuarkusPlatformDescriptor platformDescriptor = getPlatformDescriptor();
        final QuarkusJBangCodestartProjectInput input = QuarkusJBangCodestartProjectInput.builder()
                .putData(QUARKUS_BOM_GROUP_ID.getKey(), platformDescriptor.getBomGroupId())
                .putData(QUARKUS_BOM_ARTIFACT_ID.getKey(), platformDescriptor.getBomArtifactId())
                .putData(QUARKUS_BOM_VERSION.getKey(), platformDescriptor.getBomVersion())
                .build();
        final Path projectDir = testDirPath.resolve("default");
        getCatalog().createProject(input).generate(projectDir);

        assertThat(projectDir.resolve("jbang")).exists();
        assertThat(projectDir.resolve("src/GreetingResource.java")).exists();
    }

    @Test
    void generatePicocliProject() throws IOException {
        final QuarkusPlatformDescriptor platformDescriptor = getPlatformDescriptor();
        final QuarkusJBangCodestartProjectInput input = QuarkusJBangCodestartProjectInput.builder()
                .addCodestart("jbang-picocli-code")
                .putData(QUARKUS_BOM_GROUP_ID.getKey(), platformDescriptor.getBomGroupId())
                .putData(QUARKUS_BOM_ARTIFACT_ID.getKey(), platformDescriptor.getBomArtifactId())
                .putData(QUARKUS_BOM_VERSION.getKey(), platformDescriptor.getBomVersion())
                .build();
        final Path projectDir = testDirPath.resolve("picocli");
        getCatalog().createProject(input).generate(projectDir);

        assertThat(projectDir.resolve("jbang")).exists();
        assertThat(projectDir.resolve("src/GreetingCommand.java")).exists();
    }

    private QuarkusJBangCodestartCatalog getCatalog() throws IOException {
        return QuarkusJBangCodestartCatalog.fromQuarkusPlatformDescriptor(getPlatformDescriptor());
    }

}
