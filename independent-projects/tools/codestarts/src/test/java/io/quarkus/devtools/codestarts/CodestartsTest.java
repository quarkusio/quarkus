package io.quarkus.devtools.codestarts;

import static io.quarkus.devtools.codestarts.TestCodestartResourceLoader.getResource;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CodestartsTest {

    private final Path projectPath = Paths.get("target/codestarts-test");

    @BeforeEach
    void setUp() {
        FileUtils.deleteQuietly(projectPath.toFile());
        assertThat(projectPath).doesNotExist();
    }

    @Test
    void checkStaticConflictFail() throws IOException {
        final CodestartInput input = CodestartInput.builder(new TestCodestartResourceLoader())
                .build();
        final List<Codestart> allCodestarts = loadSpecific(input, "static-conflicting-file");

        final CodestartProject codestartProject = Codestarts.prepareProject(input, allCodestarts);
        Assertions.assertThatExceptionOfType(CodestartDefinitionException.class)
                .isThrownBy(() -> Codestarts.generateProject(codestartProject,
                        Files.createTempDirectory("checkStaticConflictFail")))
                .withMessageContaining("Multiple files found for path with 'fail-on-duplicate' FileStrategy:")
                .withMessageContaining(".tooling-t");
    }

    @Test
    void checkConflictFail() throws IOException {
        final CodestartInput input = CodestartInput.builder(new TestCodestartResourceLoader())
                .build();
        final List<Codestart> allCodestarts = loadSpecific(input, "conflicting-file");

        final CodestartProject codestartProject = Codestarts.prepareProject(input, allCodestarts);
        Assertions.assertThatExceptionOfType(CodestartDefinitionException.class)
                .isThrownBy(() -> Codestarts.generateProject(codestartProject, Files.createTempDirectory("checkConflictFail")))
                .withMessageContaining("Multiple files found for path with 'fail-on-duplicate' FileStrategy:")
                .withMessageContaining(".tooling-t");
    }

    @Test
    void checkConflictingFallbackProjectFail() throws IOException {
        final CodestartInput input = CodestartInput.builder(new TestCodestartResourceLoader())
                .build();
        final List<Codestart> allCodestarts = loadSpecific(input, "conflicting-fallback-project");

        Assertions.assertThatExceptionOfType(CodestartDefinitionException.class)
                .isThrownBy(() -> Codestarts.prepareProject(input, allCodestarts))
                .withMessageContaining("Multiple fallback found for a base codestart of type: 'PROJECT'");
    }

    @Test
    void checkDefaultProject() throws IOException {
        final CodestartInput input = CodestartInput.builder(new TestCodestartResourceLoader())
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);

        assertThat(codestartProject.getRequiredCodestart(CodestartSpec.Type.PROJECT).getName()).isEqualTo("foo");
        assertThat(codestartProject.getRequiredCodestart(CodestartSpec.Type.LANGUAGE).getName()).isEqualTo("a");
        assertThat(codestartProject.getRequiredCodestart(CodestartSpec.Type.BUILDTOOL).getName()).isEqualTo("y");
        assertThat(codestartProject.getRequiredCodestart(CodestartSpec.Type.CONFIG).getName()).isEqualTo("config-properties");

        assertThat(codestartProject.getExtraCodestarts()).extracting(Codestart::getResourceDir)
                .containsExactlyInAnyOrder("bundled-codestarts/tooling-t");

        final Path targetDirectory = projectPath.resolve("default-project");
        Codestarts.generateProject(codestartProject, targetDirectory);

        assertThat(targetDirectory.resolve("README.md")).hasContent("Base readme world y");
        assertThat(targetDirectory.resolve("config.properties")).hasContent("foo.bar=baz\nfoo.foo=bar\n");
        assertThat(targetDirectory.resolve(".gitignore")).hasContent("base-ignore1\nbase-ignore2\n");
        assertThat(targetDirectory.resolve("a/.tooling-t")).hasContent("a/.tooling-t");
        assertThat(targetDirectory.resolve(".tooling-t")).hasContent(".tooling-t");
        assertThat(targetDirectory.resolve("ybuild.build")).hasContent("fooa\n\nbara\n\nappend test");
    }

    @Test
    void checkSpecificProject() throws IOException {
        final TestCodestartResourceLoader resourceLoader = new TestCodestartResourceLoader();
        final CodestartInput input = CodestartInput.builder(resourceLoader)
                .addCodestart("b")
                .addCodestart("example-with-b")
                .addCodestart("maven")
                .addCodestart("config-yaml")
                .putData("project.version", "1.2.3")
                .putData("prop1", "prop-1-nonamespace")
                .putData("maven.prop2", "prop-2-namespaced")
                .putData("example-with-b.my-file-name", "my-dynamic-file-from-input")
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);

        assertThat(codestartProject.getRequiredCodestart(CodestartSpec.Type.PROJECT).getName()).isEqualTo("foo");
        assertThat(codestartProject.getRequiredCodestart(CodestartSpec.Type.LANGUAGE).getName()).isEqualTo("b");
        assertThat(codestartProject.getRequiredCodestart(CodestartSpec.Type.BUILDTOOL).getName()).isEqualTo("maven");
        assertThat(codestartProject.getRequiredCodestart(CodestartSpec.Type.CONFIG).getName()).isEqualTo("config-yaml");

        assertThat(codestartProject.getExtraCodestarts()).extracting(Codestart::getResourceDir)
                .containsExactlyInAnyOrder("bundled-codestarts/example-with-b");

        final Path targetDirectory = projectPath.resolve("specific-project");
        Codestarts.generateProject(codestartProject, targetDirectory);
        System.out.println(targetDirectory.toAbsolutePath().toString());
        assertThat(targetDirectory.resolve("README.md")).hasContent("Base readme world maven");
        assertThat(targetDirectory.resolve("config.yml")).hasContent("example: \"code\"");
        assertThat(targetDirectory.resolve(".gitignore")).hasContent("base-ignore1\nbase-ignore2\n");
        assertThat(targetDirectory.resolve("b/example-code")).hasContent("example-code");
        assertThat(targetDirectory.resolve("my-dynamic-dir/so-cool/my-dynamic-file-from-input.test"))
                .hasContent("hello my-dynamic-file-from-input");
        assertThat(targetDirectory.resolve("pom.xml"))
                .hasSameTextualContentAs(getResource("expected-pom-maven-merge.xml"));
    }

    private List<Codestart> loadSpecific(CodestartInput input, String s) throws IOException {
        return Stream.concat(
                CodestartLoader.loadBundledCodestarts(input).stream(),
                CodestartLoader.loadCodestarts(input.getResourceLoader(), s).stream())
                .collect(Collectors.toList());
    }

}
