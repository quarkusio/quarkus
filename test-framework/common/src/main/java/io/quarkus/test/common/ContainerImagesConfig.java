package io.quarkus.test.common;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

/**
 * Centralized configuration for all container images used in Quarkus tests.
 * This interface allows configuration of all container images in a single place.
 */
@ConfigMapping(prefix = "quarkus.container.images")
public interface ContainerImagesConfig {
    // Database images
    @WithName("postgres")
    String postgresImage();

    @WithName("mariadb")
    String mariadbImage();

    @WithName("db2")
    String db2Image();

    @WithName("mssql")
    String mssqlImage();

    @WithName("mysql")
    String mysqlImage();

    @WithName("oracle")
    String oracleImage();

    @WithName("mongo")
    String mongoImage();

    @WithName("redis")
    String redisImage();

    @WithName("derby")
    String derbyImage();

    @WithName("h2")
    String h2Image();

    // Elasticsearch stack
    @WithName("elasticsearch")
    String elasticsearchImage();

    @WithName("logstash")
    String logstashImage();

    @WithName("kibana")
    String kibanaImage();

    // OpenSearch
    @WithName("opensearch")
    String opensearchImage();

    // Message brokers
    @WithName("kafka")
    String kafkaImage();

    @WithName("rabbitmq")
    String rabbitmqImage();

    @WithName("amqp")
    String amqpImage();

    @WithName("pulsar")
    String pulsarImage();

    // Security
    @WithName("keycloak")
    String keycloakImage();

    @WithName("keycloak-legacy")
    String keycloakLegacyImage();

    @WithName("ldap")
    String ldapImage();

    // Miscellaneous
    @WithName("infinispan")
    String infinispanImage();

    @WithName("wiremock")
    String wiremockImage();

    @WithName("mailpit")
    String mailpitImage();

    @WithName("smtp")
    String smtpImage();

    @WithName("redpanda")
    String redpandaImage();

    @WithName("otel-lgtm")
    String otelLgtmImage();
}