package io.quarkus.test.common;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Configuration for database container images used in Quarkus tests.
 */
@ConfigMapping(prefix = "quarkus.container.image.db")
public interface DatabaseImagesConfig {


    @WithName("registry")
    @WithDefault("docker.io")
    String registry();


    @WithName("postgres")
    @WithDefault("postgres:14")
    String postgresImage();


    @WithName("mariadb")
    @WithDefault("mariadb:10.11")
    String mariadbImage();


    @WithName("db2")
    @WithDefault("ibmcom/db2:11.5.7.0a")
    String db2Image();


    @WithName("mssql")
    @WithDefault("mcr.microsoft.com/mssql/server:2022-latest")
    String mssqlImage();


    @WithName("mysql")
    @WithDefault("mysql:8.0")
    String mysqlImage();


    @WithName("oracle")
    @WithDefault("gvenzl/oracle-free:23-slim-faststart")
    String oracleImage();


    @WithName("mongo")
    @WithDefault("mongo:4.4")
    String mongoImage();


    @WithName("redis")
    @WithDefault("redis:latest")
    String redisImage();


    @WithName("derby")
    @WithDefault("apache/derby:10.15.2.0")
    String derbyImage();


    @WithName("h2")
    @WithDefault("oscarfonts/h2:2.1.214")
    String h2Image();


    default String getPostgresFullImage() {
        return registry() + "/" + postgresImage();
    }


    default String getMariaDBFullImage() {
        return registry() + "/" + mariadbImage();
    }
}