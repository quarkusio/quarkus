package io.quarkus.devservices.deployment.compose;

import static io.quarkus.devservices.deployment.compose.ComposeDevServicesProcessor.isComposeFile;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class ComposeFileTest {

    @Test
    void testComposeFileNameValidation() {
        assertTrue(isComposeFile(Path.of("/dev", "some", "dir", "docker-compose-devservice.yml")));
        assertTrue(isComposeFile(Path.of("/dev", "some", "dir", "docker-compose-devservices.yml")));
        assertTrue(isComposeFile(Path.of("/dev", "some", "dir", "docker-compose-devservices.yaml")));
        assertTrue(isComposeFile(Path.of("/dev", "some", "dir", "docker-compose-devservice-my-service.yml")));
        assertTrue(isComposeFile(Path.of("/dev", "some", "dir", "docker-compose-devservices-my-service.yaml")));
        assertTrue(isComposeFile(Path.of("/dev", "some", "dir", "compose-devservices.yml")));
        assertTrue(isComposeFile(Path.of("/dev", "some", "dir", "compose-devservices.yaml")));
        assertTrue(isComposeFile(Path.of("/dev", "some", "dir", "compose-dev-service.yaml")));
        assertTrue(isComposeFile(Path.of("/dev", "some", "dir", "compose-dev-services.yaml")));
        assertTrue(isComposeFile(Path.of("/dev", "some", "dir", "compose-devservices.yaml")));
        assertTrue(isComposeFile(Path.of("/dev", "some", "dir", "compose-devservices-my-service.yaml")));
        assertFalse(isComposeFile(Path.of("/dev", "some", "dir", "docker-compose.txt")));
        assertFalse(isComposeFile(Path.of("/dev", "some", "dir", "my-service-docker-compose.yaml")));
    }

    @Test
    void testValidComposeFileParsing() {
        File validCompose = new File(getClass().getResource("/valid-compose.yml").getFile());
        ComposeFile composeFile = new ComposeFile(validCompose);

        Map<String, ComposeServiceDefinition> services = composeFile.getServiceDefinitions();
        assertNotNull(services);
        assertEquals(3, services.size());

        // Test DB service
        ComposeServiceDefinition db = services.get("db");
        assertNotNull(db);
        assertEquals("db", db.getServiceName());
        assertEquals("database system is ready to accept connections",
                db.getLabels().get("io.quarkus.devservices.compose.wait_for.logs"));
        assertFalse(db.hasHealthCheck());

        // Test Redis service
        ComposeServiceDefinition redis = services.get("redis");
        assertNotNull(redis);
        assertEquals("redis", redis.getServiceName());
        assertTrue(redis.getPorts().stream()
                .anyMatch(p -> p.getPort() == 6379));
        assertTrue(redis.getProfiles().isEmpty());

        // Test Kafka service with profile
        ComposeServiceDefinition kafka = services.get("kafka");
        assertNotNull(kafka);
        assertEquals("kafka", kafka.getServiceName());
        assertTrue(kafka.getProfiles().contains("kafka"));
    }

    @Test
    void testInvalidComposeFile() {
        File invalidCompose = new File(getClass().getResource("/invalid-compose.yml").getFile());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new ComposeFile(invalidCompose));
        assertTrue(exception.getMessage().contains("Unable to parse YAML file"));
    }

    @Test
    void testServiceDefinitionExtraction() {
        File validCompose = new File(getClass().getResource("/valid-compose.yml").getFile());
        ComposeFile composeFile = new ComposeFile(validCompose);

        ComposeServiceDefinition db = composeFile.getServiceDefinitions().get("db");
        assertNotNull(db);

        // Test ports
        assertTrue(db.getPorts().stream()
                .anyMatch(p -> p.getPort() == 5432));

        // Test labels
        Map<String, Object> labels = db.getLabels();
        assertEquals("database system is ready to accept connections",
                labels.get("io.quarkus.devservices.compose.wait_for.logs"));

        // Test container name (should be null for valid compose)
        assertNull(db.getContainerName());
    }

    @Test
    void testLabelFormats() {
        File composeWithLabels = new File(getClass().getResource("/compose-with-labels.yml").getFile());
        ComposeFile composeFile = new ComposeFile(composeWithLabels);

        // Test array-style labels
        ComposeServiceDefinition service1 = composeFile.getServiceDefinitions().get("service1");
        Map<String, Object> labels1 = service1.getLabels();
        assertEquals("value1", labels1.get("label1"));
        assertEquals("value2", labels1.get("label2"));

        // Test map-style labels
        ComposeServiceDefinition service2 = composeFile.getServiceDefinitions().get("service2");
        Map<String, Object> labels2 = service2.getLabels();
        assertEquals("value1", labels2.get("key1"));
        assertEquals("value2", labels2.get("key2"));
    }

    @Test
    void testHealthCheck() {
        File composeWithLabels = new File(getClass().getResource("/compose-with-labels.yml").getFile());
        ComposeFile composeFile = new ComposeFile(composeWithLabels);

        // Test service with health check
        ComposeServiceDefinition service3 = composeFile.getServiceDefinitions().get("service3");
        assertTrue(service3.hasHealthCheck());

        // Test service without health check
        ComposeServiceDefinition service1 = composeFile.getServiceDefinitions().get("service1");
        assertFalse(service1.hasHealthCheck());
    }
}
