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

class QuarkusCodestartsIT extends PlatformAwareTestBase {

    private final Path projectPath = Paths.get("target/codestarts-test");

    @BeforeEach
    void setUp() throws IOException {
        ProjectTestUtil.delete(projectPath.toFile());
    }

    @Test
    void loadBundledCodestartsTest() throws IOException {
        final Collection<Codestart> codestarts = CodestartLoader
                .loadBundledCodestarts(QuarkusCodestarts.resourceLoader(getPlatformDescriptor()));
        assertThat(codestarts).hasSize(13);
        assertThat(codestarts)
                .filteredOn(c -> c.getType().isBase())
                .extracting(Codestart::getImplementedLanguages)
                .allSatisfy(s -> assertThat(s.isEmpty() || s.size() == 3).isTrue());

        assertThat(codestarts).filteredOn("ref", "commandmode")
                .extracting(Codestart::getImplementedLanguages)
                .hasSize(1)
                .allSatisfy(s -> assertThat(s).containsExactlyInAnyOrder("java", "kotlin"));
    }

    @Test
    void loadExtensionCodestartsTest() throws IOException {
        final Collection<Codestart> codestarts = CodestartLoader
                .loadCodestartsFromExtensions(QuarkusCodestarts.resourceLoader(getPlatformDescriptor()));

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
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        assertThat(codestartProject.getRequiredCodestart(CodestartType.PROJECT)).extracting(Codestart::getResourceDir)
                .isEqualTo("bundled-codestarts/project/quarkus");
        assertThat(codestartProject.getRequiredCodestart(CodestartType.BUILDTOOL)).extracting(Codestart::getResourceDir)
                .isEqualTo("bundled-codestarts/buildtool/maven");
        assertThat(codestartProject.getRequiredCodestart(CodestartType.CONFIG)).extracting(Codestart::getResourceDir)
                .isEqualTo("bundled-codestarts/config/properties");
        assertThat(codestartProject.getRequiredCodestart(CodestartType.LANGUAGE)).extracting(Codestart::getResourceDir)
                .isEqualTo("bundled-codestarts/language/java");
        assertThat(codestartProject.getBaseCodestarts()).hasSize(4);
        assertThat(codestartProject.getExtraCodestarts()).extracting(Codestart::getResourceDir)
                .isEmpty();
    }

    @Test
    void prepareProjectTestGradle() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .buildTool(BuildTool.GRADLE)
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        assertThat(codestartProject.getRequiredCodestart(CodestartType.BUILDTOOL)).extracting(Codestart::getResourceDir)
                .isEqualTo("bundled-codestarts/buildtool/gradle");
    }

    @Test
    void prepareProjectTestKotlin() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-kotlin"))
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        assertThat(codestartProject.getRequiredCodestart(CodestartType.LANGUAGE)).extracting(Codestart::getResourceDir)
                .isEqualTo("bundled-codestarts/language/kotlin");
    }

    @Test
    void prepareProjectTestScala() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-scala"))
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        assertThat(codestartProject.getRequiredCodestart(CodestartType.LANGUAGE)).extracting(Codestart::getResourceDir)
                .isEqualTo("bundled-codestarts/language/scala");
    }

    @Test
    void prepareProjectTestConfigYaml() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-config-yaml"))
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        assertThat(codestartProject.getRequiredCodestart(CodestartType.CONFIG)).extracting(Codestart::getResourceDir)
                .isEqualTo("bundled-codestarts/config/yaml");
    }

    @Test
    void prepareProjectTestResteasy() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        assertThat(codestartProject.getBaseCodestarts()).extracting(Codestart::getResourceDir)
                .contains("bundled-codestarts/config/properties");
        assertThat(codestartProject.getExtraCodestarts()).extracting(Codestart::getResourceDir)
                .containsExactlyInAnyOrder("bundled-codestarts/tooling/maven-wrapper",
                        "codestarts/resteasy-example");
    }

    @Test
    void prepareProjectTestCommandMode() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        assertThat(codestartProject.getBaseCodestarts()).extracting(Codestart::getResourceDir)
                .contains("bundled-codestarts/config/properties");
        assertThat(codestartProject.getExtraCodestarts()).extracting(Codestart::getResourceDir)
                .containsExactlyInAnyOrder("bundled-codestarts/tooling/dockerfiles",
                        "bundled-codestarts/example/commandmode-example");
    }
}
