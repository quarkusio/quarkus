package io.quarkus.deployment.builditem;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.dev.devservices.ContainerInfo;
import io.quarkus.deployment.dev.devservices.RunningContainer;

class DevServicesComposeProjectBuildItemTest {

    private DevServicesComposeProjectBuildItem buildItem;

    @BeforeEach
    void setUp() {
        // Create container info for postgres
        ContainerInfo postgresInfo = new ContainerInfo(
                "postgres123456789",
                new String[] { "postgres" },
                "postgres:13",
                "running",
                Map.of("default", new String[] { "default" }),
                Collections.emptyMap(),
                new ContainerInfo.ContainerPort[] {
                        new ContainerInfo.ContainerPort("0.0.0.0", 5432, 5432, "tcp")
                });
        RunningContainer postgresContainer = new RunningContainer(postgresInfo, Collections.emptyMap());

        // Create container info for mysql
        ContainerInfo mysqlInfo = new ContainerInfo(
                "mysql123456789",
                new String[] { "mysql" },
                "mysql:8",
                "running",
                Map.of("default", new String[] { "default" }),
                Collections.emptyMap(),
                new ContainerInfo.ContainerPort[] {
                        new ContainerInfo.ContainerPort("0.0.0.0", 3306, 3306, "tcp")
                });
        RunningContainer mysqlContainer = new RunningContainer(mysqlInfo, Collections.emptyMap());

        // Create container info for redis
        ContainerInfo redisInfo = new ContainerInfo(
                "redis123456789",
                new String[] { "redis" },
                "redis:6",
                "running",
                Map.of("default", new String[] { "default" }),
                Collections.emptyMap(),
                new ContainerInfo.ContainerPort[] {
                        new ContainerInfo.ContainerPort("0.0.0.0", 6379, 6379, "tcp")
                });
        RunningContainer redisContainer = new RunningContainer(redisInfo, Collections.emptyMap());

        // Create container info for ignored container
        Map<String, String> ignoredLabels = new HashMap<>();
        ignoredLabels.put(DevServicesComposeProjectBuildItem.COMPOSE_IGNORE, "true");
        ContainerInfo ignoredInfo = new ContainerInfo(
                "ignored123456789",
                new String[] { "ignored" },
                "ignored:latest",
                "running",
                Map.of("default", new String[] { "default" }),
                ignoredLabels,
                new ContainerInfo.ContainerPort[] {
                        new ContainerInfo.ContainerPort("0.0.0.0", 8080, 8080, "tcp")
                });
        RunningContainer ignoredContainer = new RunningContainer(ignoredInfo, Collections.emptyMap());

        // Create the build item with the containers
        Map<String, List<RunningContainer>> composeServices = new HashMap<>();
        composeServices.put("default", Arrays.asList(postgresContainer, mysqlContainer, redisContainer, ignoredContainer));

        buildItem = new DevServicesComposeProjectBuildItem("test-project", "default", composeServices, Collections.emptyMap());
    }

    @Test
    void locate() {
        // Test locating postgres container by image and port
        Optional<RunningContainer> result = buildItem.locate(List.of("postgres"), 5432);
        assertTrue(result.isPresent());
        assertEquals("postgres123456789", result.get().containerInfo().id());

        // Test locating mysql container by image and port
        result = buildItem.locate(List.of("mysql"), 3306);
        assertTrue(result.isPresent());
        assertEquals("mysql123456789", result.get().containerInfo().id());

        // Test locating with non-existent port
        // The port filtering should work correctly
        ContainerInfo postgresInfoWithoutPort = new ContainerInfo(
                "postgres123456789",
                new String[] { "postgres" },
                "postgres:13",
                "running",
                Map.of("default", new String[] { "default" }),
                Collections.emptyMap(),
                new ContainerInfo.ContainerPort[] {}); // Empty ports array
        RunningContainer postgresContainerWithoutPort = new RunningContainer(postgresInfoWithoutPort, Collections.emptyMap());

        Map<String, List<RunningContainer>> servicesWithoutPort = new HashMap<>();
        servicesWithoutPort.put("default", List.of(postgresContainerWithoutPort));
        DevServicesComposeProjectBuildItem buildItemWithoutPort = new DevServicesComposeProjectBuildItem(
                "test-project", "default", servicesWithoutPort, Collections.emptyMap());

        result = buildItemWithoutPort.locate(List.of("postgres"), 5432);
        assertTrue(result.isPresent());

        // Test locating with non-existent image
        result = buildItem.locate(List.of("nonexistent"), 5432);
        assertFalse(result.isPresent());

        // Test that ignored container is not found
        result = buildItem.locate(List.of("ignored"), 8080);
        assertFalse(result.isPresent());
    }

    @Test
    void testLocate() {
        // Test locating by image partial
        List<RunningContainer> results = buildItem.locate(List.of("postgres"));
        assertEquals(1, results.size());
        assertEquals("postgres123456789", results.get(0).containerInfo().id());

        // Test locating with multiple image partials
        results = buildItem.locate(List.of("postgres", "mysql"));
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(c -> c.containerInfo().id().equals("postgres123456789")));
        assertTrue(results.stream().anyMatch(c -> c.containerInfo().id().equals("mysql123456789")));

        // Test locating with non-existent image
        results = buildItem.locate(List.of("nonexistent"));
        assertTrue(results.isEmpty());

        // Test that ignored container is not found
        results = buildItem.locate(List.of("ignored"));
        assertTrue(results.isEmpty());
    }

    @Test
    void locateFirst() {
        // Test locating first container by image partial
        Optional<RunningContainer> result = buildItem.locateFirst(List.of("postgres"));
        assertTrue(result.isPresent());
        assertEquals("postgres123456789", result.get().containerInfo().id());

        // Test locating with multiple image partials (should return the first match)
        result = buildItem.locateFirst(List.of("postgres", "mysql"));
        assertTrue(result.isPresent());
        // The first match depends on the order of containers in the composeServices map
        // In our setup, postgres should be first
        assertEquals("postgres123456789", result.get().containerInfo().id());

        // Test locating with non-existent image
        result = buildItem.locateFirst(List.of("nonexistent"));
        assertFalse(result.isPresent());

        // Test that ignored container is not found
        result = buildItem.locateFirst(List.of("ignored"));
        assertFalse(result.isPresent());
    }
}
