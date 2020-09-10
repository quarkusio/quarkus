package io.quarkus.devtools.codestarts;

import static io.quarkus.devtools.codestarts.QuarkusCodestarts.prepareProject;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.devtools.PlatformAwareTestBase;
import io.quarkus.devtools.ProjectTestUtil;
import io.quarkus.devtools.project.BuildTool;

class QuarkusCodestartsPrepareTest extends PlatformAwareTestBase {

    private final Path projectPath = Paths.get("target/codestarts-test");

    @BeforeEach
    void setUp() throws IOException {
        ProjectTestUtil.delete(projectPath.toFile());
    }

    @Test
    void loadQuarkusCodestartsTest() throws IOException {
        final Collection<Codestart> codestarts = QuarkusCodestarts
                .loadQuarkusCodestarts(QuarkusCodestarts.resourceLoader(getPlatformDescriptor()));
        assertThat(codestarts)
                .filteredOn(c -> c.getType().isBase())
                .extracting(Codestart::getImplementedLanguages)
                .allSatisfy(s -> assertThat(s.isEmpty() || s.size() == 3).isTrue());

        assertThat(codestarts).filteredOn("ref", "commandmode")
                .extracting(Codestart::getImplementedLanguages)
                .hasSize(1)
                .allSatisfy(s -> assertThat(s).containsExactlyInAnyOrder("java", "kotlin"));

        assertThat(codestarts).filteredOn("ref", "qute")
                .extracting(Codestart::getImplementedLanguages)
                .hasSize(1)
                .allSatisfy(s -> assertThat(s).containsExactlyInAnyOrder("java", "kotlin"));

        assertThat(codestarts).filteredOn("ref", "resteasy")
                .extracting(Codestart::getImplementedLanguages)
                .hasSize(1)
                .allSatisfy(s -> assertThat(s).containsExactlyInAnyOrder("java", "kotlin", "scala"));
    }

    @Test
    void prepareProjectTestEmpty() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .noExamples()
                .noBuildToolWrapper()
                .noDockerfiles()
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        assertThat(codestartProject.getRequiredCodestart(CodestartType.PROJECT)).extracting(Codestart::getResourceDir)
                .isEqualTo("codestarts/quarkus/core/project/quarkus");
        assertThat(codestartProject.getRequiredCodestart(CodestartType.BUILDTOOL)).extracting(Codestart::getResourceDir)
                .isEqualTo("codestarts/quarkus/core/buildtool/maven");
        assertThat(codestartProject.getRequiredCodestart(CodestartType.CONFIG)).extracting(Codestart::getResourceDir)
                .isEqualTo("codestarts/quarkus/core/config/properties");
        assertThat(codestartProject.getRequiredCodestart(CodestartType.LANGUAGE)).extracting(Codestart::getResourceDir)
                .isEqualTo("codestarts/quarkus/core/language/java");
        assertThat(codestartProject.getBaseCodestarts()).hasSize(4);
        assertThat(codestartProject.getExtraCodestarts()).extracting(Codestart::getResourceDir)
                .isEmpty();
    }

    @Test
    void prepareProjectTestNoExample() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .noExamples()
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        assertThat(codestartProject.getRequiredCodestart(CodestartType.PROJECT)).extracting(Codestart::getResourceDir)
                .isEqualTo("codestarts/quarkus/core/project/quarkus");
        assertThat(codestartProject.getRequiredCodestart(CodestartType.BUILDTOOL)).extracting(Codestart::getResourceDir)
                .isEqualTo("codestarts/quarkus/core/buildtool/maven");
        assertThat(codestartProject.getRequiredCodestart(CodestartType.CONFIG)).extracting(Codestart::getResourceDir)
                .isEqualTo("codestarts/quarkus/core/config/properties");
        assertThat(codestartProject.getRequiredCodestart(CodestartType.LANGUAGE)).extracting(Codestart::getResourceDir)
                .isEqualTo("codestarts/quarkus/core/language/java");
        assertThat(codestartProject.getBaseCodestarts()).hasSize(4);
        assertThat(codestartProject.getExtraCodestarts()).extracting(Codestart::getResourceDir)
                .containsExactlyInAnyOrder("codestarts/quarkus/core/tooling/dockerfiles",
                        "codestarts/quarkus/core/tooling/maven-wrapper");
    }

    @Test
    void prepareProjectTestGradle() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .buildTool(BuildTool.GRADLE)
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        assertThat(codestartProject.getRequiredCodestart(CodestartType.BUILDTOOL)).extracting(Codestart::getResourceDir)
                .isEqualTo("codestarts/quarkus/core/buildtool/gradle");
    }

    @Test
    void prepareProjectTestKotlin() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-kotlin"))
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        assertThat(codestartProject.getRequiredCodestart(CodestartType.LANGUAGE)).extracting(Codestart::getResourceDir)
                .isEqualTo("codestarts/quarkus/core/language/kotlin");
    }

    @Test
    void prepareProjectTestScala() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-scala"))
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        assertThat(codestartProject.getRequiredCodestart(CodestartType.LANGUAGE)).extracting(Codestart::getResourceDir)
                .isEqualTo("codestarts/quarkus/core/language/scala");
    }

    @Test
    void prepareProjectTestConfigYaml() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-config-yaml"))
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        assertThat(codestartProject.getRequiredCodestart(CodestartType.CONFIG)).extracting(Codestart::getResourceDir)
                .isEqualTo("codestarts/quarkus/core/config/yaml");
    }

    @Test
    void prepareProjectTestResteasy() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        assertThat(codestartProject.getBaseCodestarts()).extracting(Codestart::getResourceDir)
                .contains("codestarts/quarkus/core/config/properties");
        assertThat(codestartProject.getExtraCodestarts()).extracting(Codestart::getResourceDir)
                .containsExactlyInAnyOrder(
                        "codestarts/quarkus/core/tooling/dockerfiles",
                        "codestarts/quarkus/core/tooling/maven-wrapper",
                        "codestarts/quarkus/core/examples/resteasy-example");
    }

    @Test
    void prepareProjectTestCommandMode() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        assertThat(codestartProject.getBaseCodestarts()).extracting(Codestart::getResourceDir)
                .contains("codestarts/quarkus/core/config/properties");
        assertThat(codestartProject.getExtraCodestarts()).extracting(Codestart::getResourceDir)
                .containsExactlyInAnyOrder(
                        "codestarts/quarkus/core/tooling/dockerfiles",
                        "codestarts/quarkus/core/tooling/maven-wrapper",
                        "codestarts/quarkus/core/examples/commandmode-example");
    }
}
