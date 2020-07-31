package io.quarkus.devtools.codestarts;

import static io.quarkus.devtools.codestarts.CodestartSpec.Type.EXAMPLE;
import static io.quarkus.devtools.codestarts.CodestartSpec.Type.PROJECT;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import org.junit.jupiter.api.Test;

class CodestartLoaderTest {

    @Test
    void testReadCodestartSpec1() throws IOException {
        final CodestartSpec codestartSpec = CodestartLoader.readCodestartSpec(resourceToString("codestart-spec1.yml",
                StandardCharsets.UTF_8, Thread.currentThread().getContextClassLoader()));

        assertThat(codestartSpec.getName()).isEqualTo("foo-project");
        assertThat(codestartSpec.getRef()).isEqualTo("foo");
        assertThat(codestartSpec.getType()).isEqualTo(PROJECT);
        assertThat(codestartSpec.getOutputStrategy()).containsEntry("foo/bar", "strategy").hasSize(1);
        assertThat(codestartSpec.isPreselected()).isTrue();
        assertThat(codestartSpec.isFallback()).isTrue();
        assertThat(codestartSpec.isExample()).isFalse();
        assertThat(codestartSpec.getLanguagesSpec()).containsOnlyKeys("base", "java");

        final CodestartSpec.LanguageSpec baseLanguageSpec = codestartSpec.getLanguagesSpec().get("base");
        assertThat(baseLanguageSpec.getDependencies()).isEmpty();
        assertThat(baseLanguageSpec.getTestDependencies()).isEmpty();
        assertThat(baseLanguageSpec.getData()).isEmpty();
        assertThat(baseLanguageSpec.getSharedData()).containsEntry("foo-shared", "bar");
        assertThat(baseLanguageSpec.getSharedData()).containsOnlyKeys("foo-shared", "project");
        assertThat(NestedMaps.getValue(baseLanguageSpec.getSharedData(), "project.group-id")).hasValue("org.acme");
        assertThat(NestedMaps.getValue(baseLanguageSpec.getSharedData(), "project.artifact-id")).hasValue("foo-project");

        final CodestartSpec.LanguageSpec javaLanguageSpec = codestartSpec.getLanguagesSpec().get("java");
        assertThat(javaLanguageSpec.getDependencies()).containsExactly(new CodestartSpec.CodestartDep("group:artifact"));
        assertThat(javaLanguageSpec.getTestDependencies())
                .containsExactly(new CodestartSpec.CodestartDep("grouptest:artifacttest:versiontest"));
        assertThat(javaLanguageSpec.getSharedData()).isEmpty();
        assertThat(javaLanguageSpec.getData()).hasSize(1).containsEntry("foo", "bar");
    }

    @Test
    void testReadCodestartSpecDefault() throws IOException {
        final CodestartSpec codestartSpec = CodestartLoader.readCodestartSpec(resourceToString("codestart-specdefault.yml",
                StandardCharsets.UTF_8, Thread.currentThread().getContextClassLoader()));

        assertThat(codestartSpec.getName()).isEqualTo("specdefault");
        assertThat(codestartSpec.getType()).isEqualTo(EXAMPLE);
        assertThat(codestartSpec.isExample()).isTrue();
        assertThat(codestartSpec.isPreselected()).isFalse();
        assertThat(codestartSpec.isFallback()).isFalse();
        assertThat(codestartSpec.getLanguagesSpec()).isEmpty();
        assertThat(codestartSpec.getOutputStrategy()).isEmpty();
    }

    @Test
    void testLoadBundledCodestarts() throws IOException {
        final CodestartInput input = CodestartInput.builder(new TestCodestartResourceLoader()).build();
        final Collection<Codestart> codestarts = CodestartLoader.loadBundledCodestarts(input);
        assertThat(codestarts).extracting(Codestart::getSpec).extracting(CodestartSpec::getName)
                .containsExactlyInAnyOrder("y", "z", "config-properties", "config-yaml", "foo", "a", "b", "replace", "t");
    }

    @Test
    void testLoadCodestartsFromExtensions() throws IOException {
        final CodestartInput input = CodestartInput.builder(new TestCodestartResourceLoader()).build();
        final Collection<Codestart> codestarts = CodestartLoader.loadCodestartsFromExtensions(input);
        assertThat(codestarts).extracting(Codestart::getSpec).extracting(CodestartSpec::getName)
                .containsExactlyInAnyOrder("example1", "example2", "example-forbidden");
    }

    @Test
    void testLoadCodestartsFail() throws IOException {
        final CodestartInput input = CodestartInput.builder(new TestCodestartResourceLoader()).build();
        assertThatExceptionOfType(CodestartDefinitionException.class)
                .isThrownBy(() -> CodestartLoader.loadCodestarts(input.getResourceLoader(), "codestarts-with-error"))
                .withMessageContaining("codestart-1");
    }

}
