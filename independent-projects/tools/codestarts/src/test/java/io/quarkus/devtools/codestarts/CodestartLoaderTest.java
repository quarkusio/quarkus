package io.quarkus.devtools.codestarts;

import static io.quarkus.devtools.codestarts.CodestartType.CODE;
import static io.quarkus.devtools.codestarts.CodestartType.PROJECT;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Set;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

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
        assertThat(codestartSpec.getTags()).isEmpty();
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
        assertThat(codestartSpec.getType()).isEqualTo(CODE);
        assertThat(codestartSpec.getTags()).isEmpty();
        assertThat(codestartSpec.isPreselected()).isFalse();
        assertThat(codestartSpec.isFallback()).isFalse();
        assertThat(codestartSpec.getLanguagesSpec()).isEmpty();
        assertThat(codestartSpec.getOutputStrategy()).isEmpty();
    }

    @Test
    void testLoadCodestartsFromDefaultDir() throws IOException {
        final Collection<Codestart> codestarts = CodestartLoader
                .loadCodestartsFromDefaultDir(new TestCodestartResourceLoader());
        assertThat(codestarts).extracting(Codestart::getName)
                .containsExactlyInAnyOrder("y", "maven", "config-properties", "config-yaml", "foo", "a", "b", "replace", "t",
                        "example-with-b", "example1", "example2", "example-forbidden");

        checkLanguages(codestarts, Sets.newSet("y", "z"), "a", "b");
        checkLanguages(codestarts, Sets.newSet("config-properties", "config-yaml", "foo", "a", "b", "replace"));
        checkLanguages(codestarts, Sets.newSet("t"), "a");
        checkLanguages(codestarts, Sets.newSet("example1"), "a");
        checkLanguages(codestarts, Sets.newSet("example2"), "b");
        checkLanguages(codestarts, Sets.newSet("example-forbidden"));
    }

    private void checkLanguages(Collection<Codestart> codestarts, Set<String> names, String... languages) {
        assertThat(codestarts)
                .filteredOn(c -> names.contains(c.getName()))
                .allSatisfy((c) -> assertThat(c)
                        .extracting(Codestart::getImplementedLanguages, as(InstanceOfAssertFactories.ITERABLE))
                        .containsExactlyInAnyOrder(languages));
    }

    @Test
    void testLoadCodestartsFromSubDir() throws IOException {
        final Collection<Codestart> codestarts = CodestartLoader.loadCodestartsFromDefaultDir(new TestCodestartResourceLoader(),
                "examples");
        assertThat(codestarts).extracting(Codestart::getName)
                .containsExactlyInAnyOrder("example1", "example2", "example-forbidden");

    }

    @Test
    void testLoadCodestartsFail() throws IOException {
        final CodestartInput input = CodestartInput.builder(new TestCodestartResourceLoader()).build();
        assertThatExceptionOfType(CodestartDefinitionException.class)
                .isThrownBy(() -> CodestartLoader.loadCodestarts(input.getResourceLoader(), "codestarts-with-error-1"))
                .withMessageContaining("codestart-1");
        assertThatExceptionOfType(CodestartDefinitionException.class)
                .isThrownBy(() -> CodestartLoader.loadCodestarts(input.getResourceLoader(), "codestarts-with-error-2"))
                .withMessageContaining("codestart-2");
        assertThatExceptionOfType(CodestartDefinitionException.class)
                .isThrownBy(() -> CodestartLoader.loadCodestarts(input.getResourceLoader(), "codestarts-with-error-3"))
                .withMessageContaining("codestart-3");
    }

}
