package io.quarkus.devtools.codestarts;

import static io.quarkus.devtools.codestarts.CodestartType.CODE;
import static io.quarkus.devtools.codestarts.CodestartType.PROJECT;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.quarkus.devtools.codestarts.core.CodestartSpec;
import io.quarkus.devtools.codestarts.utils.NestedMaps;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Set;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

class CodestartCatalogLoaderTest {

    private TestCodestartResourceLoader resourceLoader = new TestCodestartResourceLoader();

    @Test
    void testReadCodestartSpec1() throws IOException {
        final CodestartSpec codestartSpec = CodestartCatalogLoader.readCodestartSpec(resourceToString("codestart-spec1.yml",
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
        final CodestartSpec codestartSpec = CodestartCatalogLoader
                .readCodestartSpec(resourceToString("codestart-specdefault.yml",
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
        final CodestartCatalog<CodestartProjectInput> catalog = CodestartCatalogLoader
                .loadDefaultCatalog(resourceLoader, "codestarts/core", "codestarts/examples");
        assertThat(catalog.getCodestarts()).extracting(Codestart::getName)
                .containsExactlyInAnyOrder("y", "maven", "config-properties", "config-yaml", "foo", "a", "b", "replace", "t",
                        "example-with-b", "example1", "example2", "example-forbidden");

        checkLanguages(catalog, Sets.newSet("y", "z"), "a", "b");
        checkLanguages(catalog, Sets.newSet("config-properties", "config-yaml", "foo", "a", "b", "replace"));
        checkLanguages(catalog, Sets.newSet("t"), "a");
        checkLanguages(catalog, Sets.newSet("example1"), "a");
        checkLanguages(catalog, Sets.newSet("example2"), "b");
        checkLanguages(catalog, Sets.newSet("example-forbidden"));
    }

    private void checkLanguages(CodestartCatalog<?> catalog, Set<String> names, String... languages) {
        assertThat(catalog.getCodestarts())
                .filteredOn(c -> names.contains(c.getName()))
                .allSatisfy((c) -> assertThat(c)
                        .extracting(Codestart::getImplementedLanguages, as(InstanceOfAssertFactories.ITERABLE))
                        .containsExactlyInAnyOrder(languages));
    }

    @Test
    void testLoadCodestartsFromSubDir() throws IOException {
        final CodestartCatalog<CodestartProjectInput> catalog = CodestartCatalogLoader.loadDefaultCatalog(resourceLoader,
                "codestarts/examples");
        assertThat(catalog.getCodestarts()).extracting(Codestart::getName)
                .containsExactlyInAnyOrder("example1", "example2", "example-forbidden");

    }

    @Test
    void testLoadCodestartsFail() throws IOException {
        assertThatExceptionOfType(CodestartStructureException.class)
                .isThrownBy(() -> CodestartCatalogLoader.loadCodestarts(resourceLoader, "codestarts-with-error-1"))
                .withMessageContaining("codestart-1");
        assertThatExceptionOfType(CodestartStructureException.class)
                .isThrownBy(() -> CodestartCatalogLoader.loadCodestarts(resourceLoader, "codestarts-with-error-2"))
                .withMessageContaining("codestart-2");
        assertThatExceptionOfType(CodestartStructureException.class)
                .isThrownBy(() -> CodestartCatalogLoader.loadCodestarts(resourceLoader, "codestarts-with-error-3"))
                .withMessageContaining("codestart-3");
    }

    @Test
    void testGetDirName() {
        assertThat(CodestartCatalogLoader.getDirName(Paths.get("test-codestart/my-codestart/java/"))).isEqualTo("java");
        assertThat(CodestartCatalogLoader.getDirName(Paths.get("test-codestart/my-codestart/java"))).isEqualTo("java");
    }

    @Test
    void testGetResourcePath() {
        assertThat(CodestartCatalogLoader.getResourcePath("codestart")).isEqualTo("codestart");
        assertThat(CodestartCatalogLoader.getResourcePath("codestart/")).isEqualTo("codestart");
        assertThat(CodestartCatalogLoader.getResourcePath("codestart", "/my-codestart/")).isEqualTo("codestart/my-codestart");
        assertThat(CodestartCatalogLoader.getResourcePath("codestart/", "/my-codestart")).isEqualTo("codestart/my-codestart");
    }
}
