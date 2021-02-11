package io.quarkus.devtools.codestarts;

import static io.quarkus.devtools.codestarts.TestCodestartResourceLoader.getResource;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CodestartProjectGenerationTest {

    private final Path projectPath = Paths.get("target/codestarts-test");
    private TestCodestartResourceLoader resourceLoader = new TestCodestartResourceLoader();

    @BeforeEach
    void setUp() {
        FileUtils.deleteQuietly(projectPath.toFile());
        assertThat(projectPath).doesNotExist();
    }

    @Test
    void checkStaticConflictFail() throws IOException {
        final CodestartProjectInput input = CodestartProjectInput.builder()
                .build();
        final CodestartCatalog<CodestartProjectInput> catalog = loadSpecific("static-conflicting-file");

        final CodestartProjectDefinition projectDefinition = catalog.createProject(input);
        Assertions.assertThatExceptionOfType(CodestartStructureException.class)
                .isThrownBy(() -> projectDefinition.generate(Files.createTempDirectory("checkStaticConflictFail")))
                .withMessageContaining("Multiple files found for path with 'fail-on-duplicate' FileStrategy:")
                .withMessageContaining(".tooling-t");
    }

    @Test
    void checkConflictFail() throws IOException {
        final CodestartProjectInput input = CodestartProjectInput.builder()
                .build();
        final CodestartCatalog<CodestartProjectInput> catalog = loadSpecific("conflicting-file");

        final CodestartProjectDefinition projectDefinition = catalog.createProject(input);
        Assertions.assertThatExceptionOfType(CodestartStructureException.class)
                .isThrownBy(() -> projectDefinition.generate(Files.createTempDirectory("checkConflictFail")))
                .withMessageContaining("Multiple files found for path with 'fail-on-duplicate' FileStrategy:")
                .withMessageContaining(".tooling-t");
    }

    @Test
    void checkConflictingFallbackProjectFail() throws IOException {
        final CodestartProjectInput input = CodestartProjectInput.builder()
                .build();
        final CodestartCatalog<CodestartProjectInput> catalog = loadSpecific("conflicting-fallback-project");

        Assertions.assertThatExceptionOfType(CodestartStructureException.class)
                .isThrownBy(() -> catalog.createProject(input))
                .withMessageContaining("Multiple fallback found for a base codestart of type: 'PROJECT'");
    }

    @Test
    void checkDefaultProject() throws IOException {
        final CodestartProjectInput input = CodestartProjectInput.builder()
                .build();
        final CodestartProjectDefinition projectDefinition = load().createProject(input);

        assertThat(projectDefinition.getRequiredCodestart(CodestartType.PROJECT).getName()).isEqualTo("foo");
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.LANGUAGE).getName()).isEqualTo("a");
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.BUILDTOOL).getName()).isEqualTo("y");
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.CONFIG).getName()).isEqualTo("config-properties");

        assertThat(projectDefinition.getExtraCodestarts()).extracting(Codestart::getName)
                .containsExactlyInAnyOrder("t");

        final Path targetDirectory = projectPath.resolve("default-project");
        projectDefinition.generate(targetDirectory);

        assertThat(targetDirectory.resolve("README.md")).hasContent("Base readme world y");
        assertThat(targetDirectory.resolve("config.properties")).hasContent("foo.bar=baz\nfoo.foo=bar\n");
        assertThat(targetDirectory.resolve(".gitignore")).hasContent("base-ignore1\nbase-ignore2\n");
        assertThat(targetDirectory.resolve("a/.tooling-t")).hasContent("a/.tooling-t");
        assertThat(targetDirectory.resolve(".tooling-t")).hasContent(".tooling-t");
        assertThat(targetDirectory.resolve("ybuild.build")).hasContent("fooa\n\nbara\n\nappend test");
    }

    @Test
    void checkSpecificProject() throws IOException {
        final CodestartProjectInput input = CodestartProjectInput.builder()
                .addCodestart("b")
                .addCodestart("example-with-b")
                .addCodestart("maven")
                .addCodestart("config-yaml")
                .putData("project.version", "1.2.3")
                .putData("prop1", "prop-1-nonamespace")
                .putData("maven.prop2", "prop-2-namespaced")
                .putData("example-with-b.my-file-name", "my-dynamic-file-from-input")
                .build();
        final CodestartProjectDefinition projectDefinition = load().createProject(input);

        assertThat(projectDefinition.getRequiredCodestart(CodestartType.PROJECT).getName()).isEqualTo("foo");
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.LANGUAGE).getName()).isEqualTo("b");
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.BUILDTOOL).getName()).isEqualTo("maven");
        assertThat(projectDefinition.getRequiredCodestart(CodestartType.CONFIG).getName()).isEqualTo("config-yaml");

        assertThat(projectDefinition.getExtraCodestarts()).extracting(Codestart::getName)
                .containsExactlyInAnyOrder("example-with-b");

        final Path targetDirectory = projectPath.resolve("specific-project");
        projectDefinition.generate(targetDirectory);
        assertThat(targetDirectory.resolve("README.md")).hasContent("Base readme world maven");
        assertThat(targetDirectory.resolve("config.yml")).hasContent("example: \"code\"");
        assertThat(targetDirectory.resolve(".gitignore")).hasContent("base-ignore1\nbase-ignore2\n");
        assertThat(targetDirectory.resolve("b/example-code")).hasContent("example-code");
        assertThat(targetDirectory.resolve("my-dynamic-dir/so-cool/my-dynamic-file-from-input.test"))
                .hasContent("hello my-dynamic-file-from-input");
        assertThat(targetDirectory.resolve("pom.xml"))
                .hasSameTextualContentAs(getResource("expected-pom-maven-merge.xml"));
    }

    private CodestartCatalog<CodestartProjectInput> load() throws IOException {
        return CodestartCatalogLoader.loadDefaultCatalog(resourceLoader, "codestarts/core", "codestarts/examples");
    }

    private CodestartCatalog<CodestartProjectInput> loadSpecific(String s) throws IOException {
        return CodestartCatalogLoader.loadDefaultCatalog(resourceLoader, "codestarts/core", "codestarts/examples",
                "codestarts/" + s);
    }

}
