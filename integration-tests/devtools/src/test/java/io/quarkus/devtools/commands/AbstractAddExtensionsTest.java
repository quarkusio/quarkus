package io.quarkus.devtools.commands;

import static io.quarkus.platform.catalog.processor.ExtensionProcessor.isUnlisted;
import static io.quarkus.platform.tools.ToolsConstants.IO_QUARKUS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.testing.PlatformAwareTestBase;
import io.quarkus.registry.catalog.Extension;

abstract class AbstractAddExtensionsTest<T> extends PlatformAwareTestBase {

    private final Path projectPath = Paths.get("target/extensions-test");

    public Path getProjectPath() {
        return projectPath;
    }

    @Test
    void addSomeValidExtensions() throws Exception {
        createProject();
        addExtensions(asList("jdbc-postgre", "agroal", " hibernate-validator",
                "commons-io:commons-io:2.6"));
        final T project = readProject();
        hasDependency(project, "quarkus-agroal");
        hasDependency(project, "quarkus-hibernate-validator");
        hasDependency(project, "commons-io", "commons-io", "2.6");
        hasDependency(project, "quarkus-jdbc-postgresql");
    }

    @Test
    void testPartialMatches() throws Exception {
        createProject();
        addExtensions(asList("mongodb-panache", "hibernate-val", "agro"));

        final T project = readProject();
        hasDependency(project, "quarkus-agroal");
        hasDependency(project, "quarkus-mongodb-panache");
        hasDependency(project, "quarkus-hibernate-validator");
    }

    @Test
    void testRegexpMatches() throws Exception {
        createProject();

        final QuarkusCommandOutcome result = addExtensions(asList("Sm??lRye**"));

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.valueIs(AddExtensions.OUTCOME_UPDATED, true));

        final T project = readProject();

        getExtensionsWithArtifactContaining("smallrye")
                .forEach(e -> hasDependency(project, e.getArtifact().getArtifactId()));
    }

    @Test
    void addExtensionWithGroupIdAndArtifactId() throws Exception {
        createProject();

        final QuarkusCommandOutcome result = addExtensions(Collections.singletonList("io.quarkus:quarkus-agroal"));

        final T project = readProject();
        hasDependency(project, "io.quarkus", "quarkus-agroal", null);
    }

    @Test
    void addExtensionWithGAV() throws Exception {
        createProject();

        final QuarkusCommandOutcome result = addExtensions(Collections.singletonList("io.quarkus:quarkus-agroal:2.5"));

        final T project = readProject();
        hasDependency(project, "io.quarkus", "quarkus-agroal", "2.5");
    }

    @Test
    void addMissingExtension() throws Exception {
        createProject();

        final QuarkusCommandOutcome result = addExtensions(Collections.singletonList("missing"));

        final T project = readProject();
        doesNotHaveDependency(project, "quarkus-missing");
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertFalse(result.valueIs(AddExtensions.OUTCOME_UPDATED, true));
    }

    @Test
    void addExtensionTwiceInOneBatch() throws Exception {
        createProject();
        final QuarkusCommandOutcome result = addExtensions(asList("agroal", "agroal"));
        final T project = readProject();
        hasDependency(project, "quarkus-agroal");
        Assertions.assertEquals(1,
                countDependencyOccurrences(project, IO_QUARKUS, "quarkus-agroal", null));
        Assertions.assertTrue(result.valueIs(AddExtensions.OUTCOME_UPDATED, true));
        Assertions.assertTrue(result.isSuccess());
    }

    @Test
    void addExtensionTwiceInTwoBatches() throws Exception {
        createProject();

        final QuarkusCommandOutcome result1 = addExtensions(Collections.singletonList("agroal"));
        final T project1 = readProject();
        hasDependency(project1, "quarkus-agroal");
        Assertions.assertEquals(1,
                countDependencyOccurrences(project1, IO_QUARKUS, "quarkus-agroal", null));
        Assertions.assertTrue(result1.valueIs(AddExtensions.OUTCOME_UPDATED, true));
        Assertions.assertTrue(result1.isSuccess());

        final QuarkusCommandOutcome result2 = addExtensions(Collections.singletonList("agroal"));
        final T project2 = readProject();
        hasDependency(project2, "quarkus-agroal");
        Assertions.assertEquals(1,
                countDependencyOccurrences(project2, IO_QUARKUS, "quarkus-agroal", null));
        Assertions.assertFalse(result2.valueIs(AddExtensions.OUTCOME_UPDATED, true));
        Assertions.assertTrue(result2.isSuccess());
    }

    /**
     * This test reproduce the issue we had using the first selection algorithm.
     * The `arc` query was matching ArC but also hibernate-search-elasticsearch.
     */
    @Test
    void testPartialMatchConflict() throws Exception {
        // arc is now unlisted
    }

    @Test
    void addExistingAndMissingExtensionsWillFailForBoth() throws Exception {
        createProject();
        final QuarkusCommandOutcome result = addExtensions(asList("missing", "agroal"));

        final T project = readProject();
        doesNotHaveDependency(project, "quarkus-missing");
        doesNotHaveDependency(project, "quarkus-agroal");
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertFalse(result.valueIs(AddExtensions.OUTCOME_UPDATED, true));
    }

    @Test
    void doNotAddExtensionWhenMultipleMatchWithMultipleKeywords() throws Exception {
        createProject();

        addExtensions(asList("agroal", "jdbc"));

        final T project = readProject();
        doesNotHaveDependency(project, "quarkus-agroal");
        getExtensionsWithArtifactContaining("jdbc")
                .forEach(e -> doesNotHaveDependency(project, e.getArtifact().getArtifactId()));
    }

    @Test
    void doesNotAddExtensionsWhenMultipleMatchWithOneKeyword() throws Exception {
        createProject();

        addExtensions(asList("jdbc"));

        final T project = readProject();

        getExtensionsWithArtifactContaining("jdbc")
                .forEach(e -> doesNotHaveDependency(project, e.getArtifact().getArtifactId()));
    }

    @Test
    void addDuplicatedExtensionUsingGAV() throws Exception {
        createProject();

        addExtensions(asList("org.acme:acme:1", "org.acme:acme:1"));

        final T project = readProject();
        hasDependency(project, "org.acme", "acme", "1");
        Assertions.assertEquals(1,
                countDependencyOccurrences(project, "org.acme", "acme", "1"));
    }

    @Test
    void testVertx() throws Exception {
        createProject();

        addExtensions(Collections.singletonList("vertx"));

        final T project = readProject();
        hasDependency(project, "quarkus-vertx");
    }

    @Test
    void testVertxWithDot() throws Exception {
        createProject();

        // It's not possible anymore to install vert.x this way since there are more than one matching extension
        final QuarkusCommandOutcome result = addExtensions(Collections.singletonList("vert.x"));

        final T project = readProject();
        getExtensionsWithArtifactContaining("vertx")
                .forEach(e -> hasDependency(project, e.getArtifact().getArtifactId()));
    }

    private Stream<Extension> getExtensionsWithArtifactContaining(String contains) {
        return getExtensionsCatalog().getExtensions().stream()
                .filter(e -> e.getArtifact().getArtifactId().contains(contains) && !isUnlisted(e));
    }

    private void hasDependency(T project, String artifactId) {
        hasDependency(project, IO_QUARKUS, artifactId, null);
    }

    private void hasDependency(T project, String groupId, String artifactId, String version) {
        assertThat(countDependencyOccurrences(project, groupId, artifactId, version))
                .describedAs("Dependency %s:%s:%s must be in project", groupId, artifactId, version)
                .isNotZero();
    }

    private void doesNotHaveDependency(T project, String artifactId) {
        assertThat(countDependencyOccurrences(project, IO_QUARKUS, artifactId, null))
                .describedAs("Dependency %s:%s must not be in project", IO_QUARKUS, artifactId)
                .isZero();
    }

    protected abstract T createProject() throws IOException, QuarkusCommandException;

    protected abstract T readProject() throws IOException;

    protected abstract QuarkusCommandOutcome addExtensions(List<String> extensions) throws IOException, QuarkusCommandException;

    protected abstract long countDependencyOccurrences(T project, String groupId, String artifactId, String version);
}
