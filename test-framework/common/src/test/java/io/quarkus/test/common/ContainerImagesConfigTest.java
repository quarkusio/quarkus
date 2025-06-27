package io.quarkus.test.common;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class ContainerImagesConfigTest {

    @Test
    public void givenConfigurationExists_whenAccessingDatabaseImages_thenCorrectImagesAreReturned() {
        // Given the configuration properties exist
        var config = ConfigProvider.getConfig();

        // When accessing database image references
        String postgres = config.getValue("quarkus.container.images.postgres", String.class);
        String mysql = config.getValue("quarkus.container.images.mysql", String.class);
        String mariadb = config.getValue("quarkus.container.images.mariadb", String.class);

        // Then the correct image references are returned
        assertEquals("docker.io/postgres:14", postgres);
        assertEquals("docker.io/mysql:8.0", mysql);
        assertEquals("docker.io/mariadb:10.11", mariadb);
    }

    @Test
    public void givenConfigurationExists_whenAccessingElasticsearchImages_thenCorrectImagesAreReturned() {
        // Given the configuration properties exist
        var config = ConfigProvider.getConfig();

        // When accessing Elasticsearch image references
        String elasticsearch = config.getValue("quarkus.container.images.elasticsearch", String.class);
        String logstash = config.getValue("quarkus.container.images.logstash", String.class);
        String kibana = config.getValue("quarkus.container.images.kibana", String.class);

        // Then the correct image references are returned with version
        assertTrue(elasticsearch.startsWith("docker.io/elastic/elasticsearch"));
        assertTrue(logstash.startsWith("docker.io/elastic/logstash"));
        assertTrue(kibana.startsWith("docker.io/elastic/kibana"));
    }

    @Test
    public void givenConfigurationExists_whenAccessingKeycloakImages_thenQuayRegistryImagesAreReturned() {
        // Given the configuration properties exist
        var config = ConfigProvider.getConfig();

        // When accessing Keycloak image references
        String keycloak = config.getValue("quarkus.container.images.keycloak", String.class);
        String keycloakLegacy = config.getValue("quarkus.container.images.keycloak-legacy", String.class);

        // Then the Quay.io registry is used
        assertTrue(keycloak.startsWith("quay.io/keycloak/keycloak:"));
        assertTrue(keycloakLegacy.startsWith("quay.io/keycloak/keycloak:"));
        assertTrue(keycloakLegacy.endsWith("-legacy"));
    }

    @Test
    public void givenConfigurationExists_whenCheckingImageFormat_thenAllImagesHaveProperTags() {
        // Given the configuration properties exist
        var config = ConfigProvider.getConfig();

        // When accessing various image references
        String postgres = config.getValue("quarkus.container.images.postgres", String.class);
        String elasticsearch = config.getValue("quarkus.container.images.elasticsearch", String.class);
        String keycloak = config.getValue("quarkus.container.images.keycloak", String.class);

        // Then all images have proper tags
        assertTrue(postgres.contains(":"), "Postgres image should include a tag");
        assertTrue(elasticsearch.contains(":"), "Elasticsearch image should include a tag");
        assertTrue(keycloak.contains(":"), "Keycloak image should include a tag");
    }
}