package io.quarkus.devtools.codestarts;

import static io.quarkus.devtools.codestarts.QuarkusCodestarts.getToolingCodestarts;
import static io.quarkus.devtools.codestarts.QuarkusCodestarts.inputBuilder;
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
import io.quarkus.devtools.codestarts.CodestartSpec.Type;
import io.quarkus.devtools.project.BuildTool;

class CodestartsIT extends PlatformAwareTestBase {

    private final Path projectPath = Paths.get("target/codestarts-test");

    @BeforeEach
    void setUp() throws IOException {
        ProjectTestUtil.delete(projectPath.toFile());
    }

    @Test
    void loadBundledCodestartsTest() throws IOException {
        final Collection<Codestart> codestarts = CodestartLoader
                .loadBundledCodestarts(inputBuilder(getPlatformDescriptor()).build());
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
                .loadCodestartsFromExtensions(inputBuilder(getPlatformDescriptor()).build());

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
        final CodestartProject codestartProject = Codestarts
                .prepareProject(inputBuilder(getPlatformDescriptor()).includeExamples(false).build());
        assertThat(codestartProject.getRequiredCodestart(Type.PROJECT)).extracting(Codestart::getResourceDir)
                .isEqualTo("bundled-codestarts/project/quarkus");
        assertThat(codestartProject.getRequiredCodestart(Type.BUILDTOOL)).extracting(Codestart::getResourceDir)
                .isEqualTo("bundled-codestarts/buildtool/maven");
        assertThat(codestartProject.getRequiredCodestart(Type.CONFIG)).extracting(Codestart::getResourceDir)
                .isEqualTo("bundled-codestarts/config/properties");
        assertThat(codestartProject.getRequiredCodestart(Type.LANGUAGE)).extracting(Codestart::getResourceDir)
                .isEqualTo("bundled-codestarts/language/java");
        assertThat(codestartProject.getBaseCodestarts()).hasSize(4);
        assertThat(codestartProject.getExtraCodestarts()).extracting(Codestart::getResourceDir)
                .isEmpty();
    }

    @Test
    void prepareProjectTestGradle() throws IOException {
        final CodestartInput input = inputBuilder(getPlatformDescriptor())
                .addCodestarts(getToolingCodestarts(BuildTool.GRADLE, false, false))
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        assertThat(codestartProject.getRequiredCodestart(Type.BUILDTOOL)).extracting(Codestart::getResourceDir)
                .isEqualTo("bundled-codestarts/buildtool/gradle");
    }

    @Test
    void prepareProjectTestKotlin() throws IOException {
        final CodestartInput input = inputBuilder(getPlatformDescriptor())
                .addCodestarts(getToolingCodestarts(BuildTool.MAVEN, false, false))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-kotlin"))
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        assertThat(codestartProject.getRequiredCodestart(Type.LANGUAGE)).extracting(Codestart::getResourceDir)
                .isEqualTo("bundled-codestarts/language/kotlin");
    }

    @Test
    void prepareProjectTestScala() throws IOException {
        final CodestartInput input = inputBuilder(getPlatformDescriptor())
                .addCodestarts(getToolingCodestarts(BuildTool.MAVEN, false, false))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-scala"))
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        assertThat(codestartProject.getRequiredCodestart(Type.LANGUAGE)).extracting(Codestart::getResourceDir)
                .isEqualTo("bundled-codestarts/language/scala");
    }

    @Test
    void prepareProjectTestConfigYaml() throws IOException {
        final CodestartInput input = inputBuilder(getPlatformDescriptor())
                .addCodestarts(getToolingCodestarts(BuildTool.MAVEN, false, false))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-config-yaml"))
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        assertThat(codestartProject.getRequiredCodestart(Type.CONFIG)).extracting(Codestart::getResourceDir)
                .isEqualTo("bundled-codestarts/config/yaml");
    }

    @Test
    void prepareProjectTestResteasy() throws IOException {
        final CodestartInput input = inputBuilder(getPlatformDescriptor())
                .addCodestarts(getToolingCodestarts(BuildTool.MAVEN, false, true))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        assertThat(codestartProject.getBaseCodestarts()).extracting(Codestart::getResourceDir)
                .contains("bundled-codestarts/config/properties");
        assertThat(codestartProject.getExtraCodestarts()).extracting(Codestart::getResourceDir)
                .containsExactlyInAnyOrder("bundled-codestarts/tooling/maven-wrapper",
                        "codestarts/resteasy-example");
    }

    @Test
    void prepareProjectTestCommandMode() throws IOException {
        final CodestartInput input = inputBuilder(getPlatformDescriptor())
                .addCodestarts(getToolingCodestarts(BuildTool.MAVEN, true, false))
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        assertThat(codestartProject.getBaseCodestarts()).extracting(Codestart::getResourceDir)
                .contains("bundled-codestarts/config/properties");
        assertThat(codestartProject.getExtraCodestarts()).extracting(Codestart::getResourceDir)
                .containsExactlyInAnyOrder("bundled-codestarts/tooling/dockerfiles",
                        "bundled-codestarts/example/commandmode-example");
    }
}
