package io.quarkus.devtools.commands;

import static io.quarkus.platform.tools.ToolsConstants.IO_QUARKUS;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.testing.PlatformAwareTestBase;

abstract class AbstractRemoveExtensionsTest<T> extends PlatformAwareTestBase {

    private final Path projectPath = Paths.get("target/extensions-test");

    public Path getProjectPath() {
        return projectPath;
    }

    @Test
    void removeSomeValidExtensions() throws Exception {
        createProject();
        List<String> extensions = asList("jdbc-postgre", "agroal", " hibernate-validator",
                "commons-io:commons-io:2.6");
        addExtensions(extensions);
        final T project = readProject();
        hasDependency(project, "quarkus-agroal");
        hasDependency(project, "quarkus-hibernate-validator");
        hasDependency(project, "commons-io", "commons-io", "2.6");
        hasDependency(project, "quarkus-jdbc-postgresql");

        removeExtensions(extensions);
        final T projectAfter = readProject();
        hasNoDependency(projectAfter, "quarkus-agroal");
        hasNoDependency(projectAfter, "quarkus-hibernate-validator");
        hasNoDependency(projectAfter, "commons-io", "commons-io", "2.6");
        hasNoDependency(projectAfter, "quarkus-jdbc-postgresql");
    }

    @Test
    void testPartialMatches() throws Exception {
        createProject();
        List<String> extensions = asList("mongodb-panache", "hibernate-val", "agro");

        addExtensions(extensions);
        final T project = readProject();
        hasDependency(project, "quarkus-agroal");
        hasDependency(project, "quarkus-mongodb-panache");
        hasDependency(project, "quarkus-hibernate-validator");

        removeExtensions(extensions);
        final T projectAfter = readProject();
        hasNoDependency(projectAfter, "quarkus-agroal");
        hasNoDependency(projectAfter, "quarkus-mongodb-panache");
        hasNoDependency(projectAfter, "quarkus-hibernate-validator");
    }

    @Test
    void testRegexpMatches() throws Exception {
        createProject();

        final QuarkusCommandOutcome result = addExtensions(asList("Sm??lRye**"));

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.getBooleanValue(AddExtensions.OUTCOME_UPDATED));

        final QuarkusCommandOutcome result2 = removeExtensions(asList("Sm??lRye**"));

        Assertions.assertTrue(result2.isSuccess());
        Assertions.assertTrue(result2.getBooleanValue(RemoveExtensions.OUTCOME_UPDATED));

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
        hasDependency(project, IO_QUARKUS, artifactId, null);
    }

    private void hasDependency(T project, String groupId, String artifactId, String version) {
        Assertions.assertTrue(countDependencyOccurrences(project, groupId, artifactId, version) > 0);
    }

    private void hasNoDependency(T project, String artifactId) {
        hasNoDependency(project, IO_QUARKUS, artifactId, null);
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
