package io.quarkus.test.common;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.container.images")
public interface ContainerImagesConfig {
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

    @WithName("elasticsearch")
    String elasticsearchImage();

    @WithName("logstash")
    String logstashImage();

    @WithName("kibana")
    String kibanaImage();

    @WithName("opensearch")
    String opensearchImage();

    @WithName("keycloak")
    String keycloakImage();

    @WithName("keycloak-legacy")
    String keycloakLegacyImage();
}
