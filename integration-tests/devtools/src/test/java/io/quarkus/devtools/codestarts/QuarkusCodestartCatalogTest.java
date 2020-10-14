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
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.PROJECT)).extracting(Codestart::getName)
                .isEqualTo("quarkus");
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.BUILDTOOL)).extracting(Codestart::getName)
                .isEqualTo("maven");
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.CONFIG)).extracting(Codestart::getName)
                .isEqualTo("config-properties");
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.LANGUAGE)).extracting(Codestart::getName)
                .isEqualTo("java");
        assertThat(projectDefinition.getBaseCodestarts()).hasSize(4);
        assertThat(projectDefinition.getExtraCodestarts()).extracting(Codestart::getName)
                .isEmpty();
    }

    @Test
    void createProjectTestNoExample() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .noExamples()
                .build();
        final CodestartProjectDefinition projectDefinition = getCatalog().createProject(input);
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.PROJECT)).extracting(Codestart::getName)
                .isEqualTo("quarkus");
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.BUILDTOOL)).extracting(Codestart::getName)
                .isEqualTo("maven");
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.CONFIG)).extracting(Codestart::getName)
                .isEqualTo("config-properties");
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.LANGUAGE)).extracting(Codestart::getName)
                .isEqualTo("java");
        assertThat(projectDefinition.getBaseCodestarts()).hasSize(4);
        assertThat(projectDefinition.getExtraCodestarts()).extracting(Codestart::getName)
                .containsExactlyInAnyOrder("dockerfiles",
                        "maven-wrapper");
    }

    @Test
    void createProjectTestGradle() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .buildTool(BuildTool.GRADLE)
                .build();
        final CodestartProjectDefinition projectDefinition = getCatalog().createProject(input);
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.BUILDTOOL)).extracting(Codestart::getName)
                .isEqualTo("gradle");
    }

    @Test
    void createProjectTestKotlin() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-kotlin"))
                .build();
        final CodestartProjectDefinition projectDefinition = getCatalog().createProject(input);
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.LANGUAGE)).extracting(Codestart::getName)
                .isEqualTo("kotlin");
    }

    @Test
    void prepareProjectTestScala() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-scala"))
                .build();
        final CodestartProjectDefinition projectDefinition = getCatalog().createProject(input);
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.LANGUAGE)).extracting(Codestart::getName)
                .isEqualTo("scala");
    }

    @Test
    void prepareProjectTestConfigYaml() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-config-yaml"))
                .build();
        final CodestartProjectDefinition projectDefinition = getCatalog().createProject(input);
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.CONFIG)).extracting(Codestart::getName)
                .isEqualTo("config-yaml");
    }

    @Test
    void prepareProjectTestResteasy() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .build();
        final CodestartProjectDefinition projectDefinition = getCatalog().createProject(input);
        assertThat(projectDefinition.getBaseCodestarts()).extracting(Codestart::getName)
                .contains("config-properties");
        assertThat(projectDefinition.getExtraCodestarts()).extracting(Codestart::getName)
                .containsExactlyInAnyOrder(
                        "dockerfiles",
                        "maven-wrapper",
                        "resteasy-example");
    }

    @Test
    void prepareProjectTestCommandMode() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addCodestart("commandmode")
                .build();
        final CodestartProjectDefinition projectDefinition = getCatalog().createProject(input);
        assertThat(projectDefinition.getBaseCodestarts()).extracting(Codestart::getName)
                .contains("config-properties");
        assertThat(projectDefinition.getExtraCodestarts()).extracting(Codestart::getName)
                .containsExactlyInAnyOrder(
                        "dockerfiles",
                        "maven-wrapper",
                        "commandmode-example");
    }

    private QuarkusCodestartCatalog getCatalog() throws IOException {
        return QuarkusCodestartCatalog.fromQuarkusPlatformDescriptor(getPlatformDescriptor());
    }

}
