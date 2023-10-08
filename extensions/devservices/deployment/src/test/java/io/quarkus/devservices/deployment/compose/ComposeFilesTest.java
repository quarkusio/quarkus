package io.quarkus.devservices.deployment.compose;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ComposeFilesTest {

    private static final String TEST_RESOURCES = "src/test/resources/compose-files/";

    @Test
    void testLoadingSingleComposeFile() {
        File baseFile = new File(TEST_RESOURCES + "base.yml");
        ComposeFiles composeFiles = new ComposeFiles(Collections.singletonList(baseFile));

        Map<String, ComposeServiceDefinition> services = composeFiles.getServiceDefinitions();
        assertEquals(2, services.size());
        assertTrue(services.containsKey("service1"));
        assertTrue(services.containsKey("service2"));

        // Verify service1 definition
        ComposeServiceDefinition service1 = services.get("service1");
        assertEquals("service1", service1.getServiceName());
        assertTrue(service1.getPorts().stream()
                .anyMatch(p -> p.getPort() == 8080));

        // Verify service2 definition
        ComposeServiceDefinition service2 = services.get("service2");
        assertEquals("service2", service2.getServiceName());
        assertTrue(service2.getPorts().stream()
                .anyMatch(p -> p.getPort() == 8081));
    }

    @Test
    void testProjectNameHandling() {
        // Test with file containing project name
        File overrideFile = new File(TEST_RESOURCES + "override.yml");
        ComposeFiles composeFiles = new ComposeFiles(Collections.singletonList(overrideFile));
        assertEquals("test-project", composeFiles.getProjectName());

        // Test with file without project name
        File baseFile = new File(TEST_RESOURCES + "base.yml");
        composeFiles = new ComposeFiles(Collections.singletonList(baseFile));
        assertNull(composeFiles.getProjectName());
    }

    @Test
    void testServiceNameConflict() {
        File baseFile = new File(TEST_RESOURCES + "base.yml");
        File overrideFile = new File(TEST_RESOURCES + "override.yml");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ComposeFiles(Arrays.asList(baseFile, overrideFile)));
        assertTrue(exception.getMessage().contains("Service name conflict"));
        assertTrue(exception.getMessage().contains("service1"));
    }

    @Test
    void testMultipleNonConflictingFiles() {
        File baseFile = new File(TEST_RESOURCES + "base.yml");
        File extensionFile = new File(TEST_RESOURCES + "extension.yml");

        ComposeFiles composeFiles = new ComposeFiles(Arrays.asList(baseFile, extensionFile));

        // Verify all services are loaded
        Set<String> serviceNames = composeFiles.getAllServiceNames();
        assertEquals(4, serviceNames.size());
        assertTrue(serviceNames.containsAll(Arrays.asList("service1", "service2", "service3", "service4")));

        // Verify service definitions
        Map<String, ComposeServiceDefinition> services = composeFiles.getServiceDefinitions();
        assertEquals(4, services.size());

        // Verify a service from each file
        ComposeServiceDefinition service2 = services.get("service2");
        assertTrue(service2.getPorts().stream()
                .anyMatch(p -> p.getPort() == 8081));

        ComposeServiceDefinition service3 = services.get("service3");
        assertTrue(service3.getPorts().stream()
                .anyMatch(p -> p.getPort() == 8082));
    }

    @Test
    void testEmptyFileList() {
        ComposeFiles composeFiles = new ComposeFiles(Collections.emptyList());
        assertTrue(composeFiles.getAllServiceNames().isEmpty());
        assertTrue(composeFiles.getServiceDefinitions().isEmpty());
    }

    @Test
    void testNonExistentFile() {
        File nonExistentFile = new File(TEST_RESOURCES + "non-existent.yml");
        assertThrows(
                IllegalArgumentException.class,
                () -> new ComposeFiles(Collections.singletonList(nonExistentFile)));
    }

    @Test
    void testGetFiles() {
        File baseFile = new File(TEST_RESOURCES + "base.yml");
        File extensionFile = new File(TEST_RESOURCES + "extension.yml");
        List<File> inputFiles = Arrays.asList(baseFile, extensionFile);

        ComposeFiles composeFiles = new ComposeFiles(inputFiles);
        List<File> returnedFiles = composeFiles.getFiles();

        assertEquals(inputFiles.size(), returnedFiles.size());
        assertTrue(returnedFiles.containsAll(inputFiles));
    }
}