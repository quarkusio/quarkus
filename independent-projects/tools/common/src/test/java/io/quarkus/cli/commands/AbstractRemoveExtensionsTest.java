package io.quarkus.cli.commands;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

abstract class AbstractRemoveExtensionsTest<T> extends PlatformAwareTestBase {

    private final Path projectPath = Paths.get("target/extensions-test");

    public Path getProjectPath() {
        return projectPath;
    }

    @Test
    void removeSomeValidExtensions() throws Exception {
        createProject();
        List<String> extensions = asList("jdbc-postgre", "agroal", "quarkus-arc", " hibernate-validator",
                "commons-io:commons-io:2.6");
        addExtensions(extensions);
        final T project = readProject();
        hasDependency(project, "quarkus-agroal");
        hasDependency(project, "quarkus-arc");
        hasDependency(project, "quarkus-hibernate-validator");
        hasDependency(project, "commons-io", "commons-io", "2.6");
        hasDependency(project, "quarkus-jdbc-postgresql");

        removeExtensions(extensions);
        final T projectAfter = readProject();
        hasNoDependency(projectAfter, "quarkus-agroal");
        hasNoDependency(projectAfter, "quarkus-arc");
        hasNoDependency(projectAfter, "quarkus-hibernate-validator");
        hasNoDependency(projectAfter, "commons-io", "commons-io", "2.6");
        hasNoDependency(projectAfter, "quarkus-jdbc-postgresql");
    }

    @Test
    void testPartialMatches() throws Exception {
        createProject();
        List<String> extensions = asList("orm-pana", "jdbc-postgre", "arc");

        addExtensions(extensions);
        final T project = readProject();
        hasDependency(project, "quarkus-arc");
        hasDependency(project, "quarkus-hibernate-orm-panache");
        hasDependency(project, "quarkus-jdbc-postgresql");

        removeExtensions(extensions);
        final T projectAfter = readProject();
        hasNoDependency(projectAfter, "quarkus-arc");
        hasNoDependency(projectAfter, "quarkus-hibernate-orm-panache");
        hasNoDependency(projectAfter, "quarkus-jdbc-postgresql");
    }

    @Test
    void testRegexpMatches() throws Exception {
        createProject();

        final QuarkusCommandOutcome result = addExtensions(asList("Sm??lRye**"));

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.valueIs(AddExtensions.OUTCOME_UPDATED, true));

        final QuarkusCommandOutcome result2 = removeExtensions(asList("Sm??lRye**"));

        Assertions.assertTrue(result2.isSuccess());
        Assertions.assertTrue(result2.valueIs(RemoveExtensions.OUTCOME_UPDATED, true));

        final T project = readProject();
        hasNoDependency(project, "quarkus-smallrye-reactive-messaging");
        hasNoDependency(project, "quarkus-smallrye-reactive-streams-operators");
        hasNoDependency(project, "quarkus-smallrye-opentracing");
        hasNoDependency(project, "quarkus-smallrye-metrics");
        hasNoDependency(project, "quarkus-smallrye-reactive-messaging-kafka");
        hasNoDependency(project, "quarkus-smallrye-health");
        hasNoDependency(project, "quarkus-smallrye-openapi");
        hasNoDependency(project, "quarkus-smallrye-jwt");
        hasNoDependency(project, "quarkus-smallrye-context-propagation");
        hasNoDependency(project, "quarkus-smallrye-reactive-type-converters");
        hasNoDependency(project, "quarkus-smallrye-reactive-messaging-amqp");
        hasNoDependency(project, "quarkus-smallrye-fault-tolerance");
    }

    private void hasDependency(T project, String artifactId) {
        hasDependency(project, getPluginGroupId(), artifactId, null);
    }

    private void hasDependency(T project, String groupId, String artifactId, String version) {
        Assertions.assertTrue(countDependencyOccurrences(project, groupId, artifactId, version) > 0);
    }

    private void hasNoDependency(T project, String artifactId) {
        hasNoDependency(project, getPluginGroupId(), artifactId, null);
    }

    private void hasNoDependency(T project, String groupId, String artifactId, String version) {
        Assertions.assertTrue(countDependencyOccurrences(project, groupId, artifactId, version) == 0);
    }

    protected abstract T createProject() throws IOException, QuarkusCommandException;

    protected abstract T readProject() throws IOException;

    protected abstract QuarkusCommandOutcome addExtensions(List<String> extensions) throws IOException, QuarkusCommandException;

    protected abstract long countDependencyOccurrences(T project, String groupId, String artifactId, String version);

    protected abstract QuarkusCommandOutcome removeExtensions(List<String> extensions)
            throws IOException, QuarkusCommandException;
}
