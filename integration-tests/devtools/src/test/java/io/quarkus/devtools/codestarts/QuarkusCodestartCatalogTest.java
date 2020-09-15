package io.quarkus.devtools.codestarts;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.devtools.PlatformAwareTestBase;
import io.quarkus.devtools.ProjectTestUtil;
import io.quarkus.devtools.project.BuildTool;

class QuarkusCodestartCatalogTest extends PlatformAwareTestBase {

    private final Path projectPath = Paths.get("target/codestarts-test");

    @BeforeEach
    void setUp() throws IOException {
        ProjectTestUtil.delete(projectPath.toFile());
    }

    @Test
    void loadQuarkusCodestartsTest() throws IOException {
        final QuarkusCodestartCatalog catalog = getCatalog();
        assertThat(catalog.getCodestarts())
                .filteredOn(c -> c.getType().isBase())
                .extracting(Codestart::getImplementedLanguages)
                .allSatisfy(s -> assertThat(s.isEmpty() || s.size() == 3).isTrue());

        assertThat(catalog.getCodestarts()).filteredOn("ref", "commandmode")
                .extracting(Codestart::getImplementedLanguages)
                .hasSize(1)
                .allSatisfy(s -> assertThat(s).containsExactlyInAnyOrder("java", "kotlin"));

        assertThat(catalog.getCodestarts()).filteredOn("ref", "qute")
                .extracting(Codestart::getImplementedLanguages)
                .hasSize(1)
                .allSatisfy(s -> assertThat(s).containsExactlyInAnyOrder("java", "kotlin"));

        assertThat(catalog.getCodestarts()).filteredOn("ref", "resteasy")
                .extracting(Codestart::getImplementedLanguages)
                .hasSize(1)
                .allSatisfy(s -> assertThat(s).containsExactlyInAnyOrder("java", "kotlin", "scala"));
    }

    @Test
    void createProjectTestEmpty() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .noExamples()
                .noBuildToolWrapper()
                .noDockerfiles()
                .build();
        final CodestartProjectDefinition projectDefinition = getCatalog().createProject(input);
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.PROJECT)).extracting(Codestart::getResourceDir)
                .isEqualTo("codestarts/quarkus/core/project/quarkus");
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.BUILDTOOL)).extracting(Codestart::getResourceDir)
                .isEqualTo("codestarts/quarkus/core/buildtool/maven");
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.CONFIG)).extracting(Codestart::getResourceDir)
                .isEqualTo("codestarts/quarkus/core/config/properties");
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.LANGUAGE)).extracting(Codestart::getResourceDir)
                .isEqualTo("codestarts/quarkus/core/language/java");
        assertThat(projectDefinition.getBaseCodestarts()).hasSize(4);
        assertThat(projectDefinition.getExtraCodestarts()).extracting(Codestart::getResourceDir)
                .isEmpty();
    }

    @Test
    void createProjectTestNoExample() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .noExamples()
                .build();
        final CodestartProjectDefinition projectDefinition = getCatalog().createProject(input);
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.PROJECT)).extracting(Codestart::getResourceDir)
                .isEqualTo("codestarts/quarkus/core/project/quarkus");
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.BUILDTOOL)).extracting(Codestart::getResourceDir)
                .isEqualTo("codestarts/quarkus/core/buildtool/maven");
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.CONFIG)).extracting(Codestart::getResourceDir)
                .isEqualTo("codestarts/quarkus/core/config/properties");
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.LANGUAGE)).extracting(Codestart::getResourceDir)
                .isEqualTo("codestarts/quarkus/core/language/java");
        assertThat(projectDefinition.getBaseCodestarts()).hasSize(4);
        assertThat(projectDefinition.getExtraCodestarts()).extracting(Codestart::getResourceDir)
                .containsExactlyInAnyOrder("codestarts/quarkus/core/tooling/dockerfiles",
                        "codestarts/quarkus/core/tooling/maven-wrapper");
    }

    @Test
    void createProjectTestGradle() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .buildTool(BuildTool.GRADLE)
                .build();
        final CodestartProjectDefinition projectDefinition = getCatalog().createProject(input);
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.BUILDTOOL)).extracting(Codestart::getResourceDir)
                .isEqualTo("codestarts/quarkus/core/buildtool/gradle");
    }

    @Test
    void createProjectTestKotlin() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-kotlin"))
                .build();
        final CodestartProjectDefinition projectDefinition = getCatalog().createProject(input);
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.LANGUAGE)).extracting(Codestart::getResourceDir)
                .isEqualTo("codestarts/quarkus/core/language/kotlin");
    }

    @Test
    void prepareProjectTestScala() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-scala"))
                .build();
        final CodestartProjectDefinition projectDefinition = getCatalog().createProject(input);
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.LANGUAGE)).extracting(Codestart::getResourceDir)
                .isEqualTo("codestarts/quarkus/core/language/scala");
    }

    @Test
    void prepareProjectTestConfigYaml() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-config-yaml"))
                .build();
        final CodestartProjectDefinition projectDefinition = getCatalog().createProject(input);
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.CONFIG)).extracting(Codestart::getResourceDir)
                .isEqualTo("codestarts/quarkus/core/config/yaml");
    }

    @Test
    void prepareProjectTestResteasy() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .build();
        final CodestartProjectDefinition projectDefinition = getCatalog().createProject(input);
        assertThat(projectDefinition.getBaseCodestarts()).extracting(Codestart::getResourceDir)
                .contains("codestarts/quarkus/core/config/properties");
        assertThat(projectDefinition.getExtraCodestarts()).extracting(Codestart::getResourceDir)
                .containsExactlyInAnyOrder(
                        "codestarts/quarkus/core/tooling/dockerfiles",
                        "codestarts/quarkus/core/tooling/maven-wrapper",
                        "codestarts/quarkus/core/examples/resteasy-example");
    }

    @Test
    void prepareProjectTestCommandMode() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .build();
        final CodestartProjectDefinition projectDefinition = getCatalog().createProject(input);
        assertThat(projectDefinition.getBaseCodestarts()).extracting(Codestart::getResourceDir)
                .contains("codestarts/quarkus/core/config/properties");
        assertThat(projectDefinition.getExtraCodestarts()).extracting(Codestart::getResourceDir)
                .containsExactlyInAnyOrder(
                        "codestarts/quarkus/core/tooling/dockerfiles",
                        "codestarts/quarkus/core/tooling/maven-wrapper",
                        "codestarts/quarkus/core/examples/commandmode-example");
    }

    private QuarkusCodestartCatalog getCatalog() throws IOException {
        return QuarkusCodestartCatalog.fromQuarkusPlatformDescriptor(getPlatformDescriptor());
    }

}
