package io.quarkus.devtools.commands;

import static java.util.Arrays.asList;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

abstract class AbstractAddExtensionsTest<T> extends PlatformAwareTestBase {

    private final Path projectPath = Paths.get("target/extensions-test");

    public Path getProjectPath() {
        return projectPath;
    }

    @Test
    void addSomeValidExtensions() throws Exception {
        createProject();
        addExtensions(asList("jdbc-postgre", "agroal", "quarkus-arc", " hibernate-validator",
                "commons-io:commons-io:2.6"));
        final T project = readProject();
        hasDependency(project, "quarkus-agroal");
        hasDependency(project, "quarkus-arc");
        hasDependency(project, "quarkus-hibernate-validator");
        hasDependency(project, "commons-io", "commons-io", "2.6");
        hasDependency(project, "quarkus-jdbc-postgresql");
    }

    @Test
    void testPartialMatches() throws Exception {
        createProject();
        addExtensions(asList("orm-pana", "jdbc-postgre", "arc"));

        final T project = readProject();
        hasDependency(project, "quarkus-arc");
        hasDependency(project, "quarkus-hibernate-orm-panache");
        hasDependency(project, "quarkus-jdbc-postgresql");
    }

    @Test
    void testRegexpMatches() throws Exception {
        createProject();

        final QuarkusCommandOutcome result = addExtensions(asList("Sm??lRye**"));

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.valueIs(AddExtensions.OUTCOME_UPDATED, true));

        final T project = readProject();
        hasDependency(project, "quarkus-smallrye-reactive-messaging");
        hasDependency(project, "quarkus-smallrye-reactive-streams-operators");
        hasDependency(project, "quarkus-smallrye-opentracing");
        hasDependency(project, "quarkus-smallrye-metrics");
        hasDependency(project, "quarkus-smallrye-reactive-messaging-kafka");
        hasDependency(project, "quarkus-smallrye-health");
        hasDependency(project, "quarkus-smallrye-openapi");
        hasDependency(project, "quarkus-smallrye-jwt");
        hasDependency(project, "quarkus-smallrye-context-propagation");
        hasDependency(project, "quarkus-smallrye-reactive-type-converters");
        hasDependency(project, "quarkus-smallrye-reactive-messaging-amqp");
        hasDependency(project, "quarkus-smallrye-fault-tolerance");
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
                countDependencyOccurrences(project, getPluginGroupId(), "quarkus-agroal", null));
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
                countDependencyOccurrences(project1, getPluginGroupId(), "quarkus-agroal", null));
        Assertions.assertTrue(result1.valueIs(AddExtensions.OUTCOME_UPDATED, true));
        Assertions.assertTrue(result1.isSuccess());

        final QuarkusCommandOutcome result2 = addExtensions(Collections.singletonList("agroal"));
        final T project2 = readProject();
        hasDependency(project2, "quarkus-agroal");
        Assertions.assertEquals(1,
                countDependencyOccurrences(project2, getPluginGroupId(), "quarkus-agroal", null));
        Assertions.assertFalse(result2.valueIs(AddExtensions.OUTCOME_UPDATED, true));
        Assertions.assertTrue(result2.isSuccess());
    }

    /**
     * This test reproduce the issue we had using the first selection algorithm.
     * The `arc` query was matching ArC but also hibernate-search-elasticsearch.
     */
    @Test
    void testPartialMatchConflict() throws Exception {
        createProject();

        final QuarkusCommandOutcome result = addExtensions(Collections.singletonList("arc"));

        Assertions.assertTrue(result.valueIs(AddExtensions.OUTCOME_UPDATED, true));
        Assertions.assertTrue(result.isSuccess());
        final T project = readProject();
        hasDependency(project, "quarkus-arc");

        final QuarkusCommandOutcome result2 = addExtensions(Collections.singletonList("elasticsearch"));

        Assertions.assertTrue(result2.valueIs(AddExtensions.OUTCOME_UPDATED, true));
        Assertions.assertTrue(result2.isSuccess());
        final T project2 = readProject();
        hasDependency(project2, "quarkus-hibernate-search-elasticsearch");
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
    void doNotAddExtensionWhenMultipleMatch() throws Exception {
        createProject();

        addExtensions(asList("agroal", "jdbc"));

        final T project = readProject();
        doesNotHaveDependency(project, "quarkus-agroal");
        doesNotHaveDependency(project, "quarkus-jdbc-postgresql");
        doesNotHaveDependency(project, "quarkus-jdbc-h2");
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

        addExtensions(Collections.singletonList("vert.x"));

        final T project = readProject();
        hasDependency(project, "quarkus-vertx");
    }

    private void hasDependency(T project, String artifactId) {
        hasDependency(project, getPluginGroupId(), artifactId, null);
    }

    private void hasDependency(T project, String groupId, String artifactId, String version) {
        Assertions.assertTrue(countDependencyOccurrences(project, groupId, artifactId, version) > 0);
    }

    private void doesNotHaveDependency(T project, String artifactId) {
        Assertions.assertTrue(countDependencyOccurrences(project, getPluginGroupId(), artifactId, null) == 0);
    }

    protected abstract T createProject() throws IOException, QuarkusCommandException;

    protected abstract T readProject() throws IOException;

    protected abstract QuarkusCommandOutcome addExtensions(List<String> extensions) throws IOException, QuarkusCommandException;

    protected abstract long countDependencyOccurrences(T project, String groupId, String artifactId, String version);
}
