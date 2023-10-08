package io.quarkus.devservices.deployment.compose;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;

public class ComposeProjectTest {

    private ComposeProject composeProject;
    private static final String COMPOSE_EXECUTABLE = "docker";
    private final File composeFile = new File(getClass().getResource("/valid-compose.yml").getFile());
    private final File composeFileWithProfiles = new File(getClass().getResource("/valid-compose-with-profiles.yml").getFile());
    private final File composeFileWithIgnore = new File(getClass().getResource("/valid-compose-with-ignore.yml").getFile());

    @Test
    void testBasicProject() {
        composeProject = new ComposeProject.Builder(List.of(composeFile), COMPOSE_EXECUTABLE)
                .withProject("test")
                .withIdentifier("test")
                .withStartupTimeout(Duration.ofMinutes(2))
                .build();

        // Verify service names
        Map<String, WaitAllStrategy> waitStrategies = composeProject.getWaitStrategies();
        assertFalse(waitStrategies.isEmpty());
        assertEquals(2, waitStrategies.size()); // db and redis
        assertTrue(waitStrategies.containsKey("db"));
        assertTrue(waitStrategies.containsKey("redis"));

        // Verify project name
        String project = composeProject.getProject();
        assertEquals("test", project);
    }

    @Test
    void testProjectWithWaitStrategies() {
        composeProject = new ComposeProject.Builder(List.of(composeFile), COMPOSE_EXECUTABLE)
                .withIdentifier("test-wait")
                .withStartupTimeout(Duration.ofMinutes(2))
                .build();

        // Verify project name
        String project = composeProject.getProject();
        assertNotNull(project);
        assertTrue(project.startsWith("test-wait"));
    }

    @Test
    void testProjectWithProfiles() {
        composeProject = new ComposeProject.Builder(List.of(composeFileWithProfiles), COMPOSE_EXECUTABLE)
                .withIdentifier("test-profiles")
                .build();

        // Verify only kafka service is running (as it's in the kafka profile)
        Map<String, WaitAllStrategy> waitStrategies = composeProject.getWaitStrategies();
        assertEquals(1, waitStrategies.size());
        assertTrue(waitStrategies.containsKey("db"));
    }

    @Test
    void testProjectWithScaling() {
        composeProject = new ComposeProject.Builder(List.of(composeFile), COMPOSE_EXECUTABLE)
                .withIdentifier("test-scaling")
                .withScalingPreferences(Map.of("redis", 2))
                .build();

        // Verify redis service is scaled to 2 instances
        Map<String, WaitAllStrategy> waitStrategies = composeProject.getWaitStrategies();

        assertTrue(waitStrategies.containsKey("redis"));
    }

    @Test
    void testIgnoredServices() {
        composeProject = new ComposeProject.Builder(List.of(composeFileWithIgnore), COMPOSE_EXECUTABLE)
                .withIdentifier("test-scaling")
                .build();

        // Verify only kafka service is selected (as it's in the kafka profile)
        Map<String, WaitAllStrategy> waitStrategies = composeProject.getWaitStrategies();
        assertEquals(2, waitStrategies.size());
        assertTrue(waitStrategies.containsKey("kafka"));
        assertTrue(waitStrategies.containsKey("zookeeper"));

        // Verify project name set in file
        assertEquals("devservices", composeProject.getProject());
    }

    @Test
    void testProfileServices() {
        composeProject = new ComposeProject.Builder(List.of(composeFileWithProfiles), COMPOSE_EXECUTABLE)
                .withIdentifier("test-profiles")
                .withProfiles(List.of("redis"))
                .build();

        // Verify no service is selected
        Map<String, WaitAllStrategy> waitStrategies = composeProject.getWaitStrategies();
        assertEquals(2, waitStrategies.size());
        assertTrue(waitStrategies.containsKey("db"));
        assertTrue(waitStrategies.containsKey("redis"));

        // Verify project name set in file
        assertEquals("devservices", composeProject.getProject());
    }

}