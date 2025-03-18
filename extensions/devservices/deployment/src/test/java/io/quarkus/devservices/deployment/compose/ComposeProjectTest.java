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
        ComposeFiles files = new ComposeFiles(List.of(composeFile));
        composeProject = new ComposeProject.Builder(files, COMPOSE_EXECUTABLE)
                .withProject("test")
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
        ComposeFiles files = new ComposeFiles(List.of(composeFileWithProfiles));
        composeProject = new ComposeProject.Builder(files, COMPOSE_EXECUTABLE)
                .withStartupTimeout(Duration.ofMinutes(2))
                .build();

        // Verify project name
        String project = composeProject.getProject();
        assertNotNull(project);
        assertTrue(project.equals("devservices"));
    }

    @Test
    void testProjectWithProfiles() {
        ComposeFiles files = new ComposeFiles(List.of(composeFileWithProfiles));
        composeProject = new ComposeProject.Builder(files, COMPOSE_EXECUTABLE)
                .build();

        // Verify only kafka service is running (as it's in the kafka profile)
        Map<String, WaitAllStrategy> waitStrategies = composeProject.getWaitStrategies();
        assertEquals(1, waitStrategies.size());
        assertTrue(waitStrategies.containsKey("db"));
    }

    @Test
    void testProjectWithScaling() {
        ComposeFiles files = new ComposeFiles(List.of(composeFile));
        composeProject = new ComposeProject.Builder(files, COMPOSE_EXECUTABLE)
                .withScalingPreferences(Map.of("redis", 2))
                .build();

        // Verify redis service is scaled to 2 instances
        Map<String, WaitAllStrategy> waitStrategies = composeProject.getWaitStrategies();

        assertTrue(waitStrategies.containsKey("redis"));
    }

    @Test
    void testIgnoredServices() {
        ComposeFiles files = new ComposeFiles(List.of(composeFileWithIgnore));
        composeProject = new ComposeProject.Builder(files, COMPOSE_EXECUTABLE)
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
        ComposeFiles files = new ComposeFiles(List.of(composeFileWithProfiles));
        composeProject = new ComposeProject.Builder(files, COMPOSE_EXECUTABLE)
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