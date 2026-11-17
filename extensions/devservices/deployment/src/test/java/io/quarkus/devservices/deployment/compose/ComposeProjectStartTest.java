package io.quarkus.devservices.deployment.compose;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Verifies the order in which {@link ComposeProject#start()} runs compose commands and copies the exposed ports file.
 * A service waiting on that file to become healthy would otherwise block a {@code depends_on} with
 * {@code condition: service_healthy} forever.
 */
class ComposeProjectStartTest {

    private final File healthyDependencyFile = new File(
            getClass().getResource("/compose-with-exposed-ports-and-healthy-dependency.yml").getFile());
    private final File plainFile = new File(getClass().getResource("/valid-compose.yml").getFile());

    @Test
    void exposedPortsAreCopiedBeforeServicesWaitingOnHealthyDependenciesAreStarted() {
        RecordingComposeProject project = new RecordingComposeProject(healthyDependencyFile, List.of(), Map.of(), false);

        project.start();

        assertThat(project.events).containsExactly(
                "run: compose up -d kafka",
                "list-instances follow-logs=false",
                "copy-exposed-ports",
                "run: compose up -d",
                "discover");
    }

    @Test
    void containerLogsAreOnlyFollowedByTheFinalDiscovery() {
        RecordingComposeProject project = new RecordingComposeProject(healthyDependencyFile, List.of(), Map.of(), true);

        project.start();

        assertThat(project.events).containsExactly(
                "run: compose up -d kafka",
                "list-instances follow-logs=false",
                "copy-exposed-ports",
                "run: compose up -d",
                "discover");
    }

    @Test
    void servicesOutsideTheActiveProfilesAreNotStartedEarly() {
        RecordingComposeProject project = new RecordingComposeProject(healthyDependencyFile, List.of("analytics"),
                Map.of(), false);

        project.start();

        assertThat(project.events).contains("run: compose up -d analytics kafka");
        assertThat(project.events).doesNotContain("run: compose up -d kafka");
    }

    @Test
    void scalingOfOtherServicesIsOnlyAppliedWhenStartingTheWholeProject() {
        RecordingComposeProject project = new RecordingComposeProject(healthyDependencyFile, List.of(),
                Map.of("consumer", 2), false);

        project.start();

        assertThat(project.events).contains("run: compose up -d kafka", "run: compose up -d --scale consumer=2");
        assertThat(project.events).noneMatch(event -> event.startsWith("run: compose up -d --scale consumer=2 "));
    }

    @Test
    void projectsWithoutExposedPortsAreStartedWithASingleCommand() {
        RecordingComposeProject project = new RecordingComposeProject(plainFile, List.of(), Map.of(), false);

        project.start();

        assertThat(project.events).containsExactly("run: compose up -d", "discover");
    }

    private static class RecordingComposeProject extends ComposeProject {

        private final List<String> events = new ArrayList<>();

        RecordingComposeProject(File composeFile, List<String> profiles, Map<String, Integer> scalingPreferences,
                boolean followContainerLogs) {
            super(null, new ComposeFiles(List.of(composeFile)), "docker", "test", Duration.ofMinutes(1),
                    Duration.ofSeconds(10), true, false, followContainerLogs, true, null, null, List.of(), profiles,
                    scalingPreferences, Map.of());
        }

        @Override
        public void runWithCompose(String cmd, Map<String, String> env) {
            events.add("run: " + cmd);
        }

        @Override
        public synchronized void discoverServiceInstances(boolean checkForRequiredServices) {
            events.add("discover");
        }

        @Override
        List<ComposeServiceWaitStrategyTarget> listRunningServiceInstances(boolean followLogs) {
            events.add("list-instances follow-logs=" + followLogs);
            return List.of();
        }

        @Override
        void copyExposedPortsToContainers(List<ComposeServiceWaitStrategyTarget> instances) {
            events.add("copy-exposed-ports");
        }
    }
}
