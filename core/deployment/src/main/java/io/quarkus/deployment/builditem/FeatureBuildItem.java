package io.quarkus.deployment.builditem;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Describes a functionality provided by an extension. The info is displayed to users.
 */
public final class FeatureBuildItem extends MultiBuildItem {

    public static final String AGROAL = "agroal";
    public static final String AMAZON_LAMBDA = "amazon-lambda";
    public static final String ARTEMIS_CORE = "artemis-core";
    public static final String ARTEMIS_JMS = "artemis-jms";
    public static final String CACHE = "cache";
    public static final String CDI = "cdi";
    public static final String CONFIG_YAML = "config-yaml";
    public static final String DYNAMODB = "dynamodb";
    public static final String ELASTICSEARCH_REST_CLIENT = "elasticsearch-rest-client";
    public static final String FLYWAY = "flyway";
    public static final String HIBERNATE_ORM = "hibernate-orm";
    public static final String HIBERNATE_ORM_PANACHE = "hibernate-orm-panache";
    public static final String HIBERNATE_VALIDATOR = "hibernate-validator";
    public static final String HIBERNATE_SEARCH_ELASTICSEARCH = "hibernate-search-elasticsearch";
    public static final String INFINISPAN_CLIENT = "infinispan-client";
    public static final String INFINISPAN_EMBEDDED = "infinispan-embedded";
    public static final String JAEGER = "jaeger";
    public static final String JDBC_DERBY = "jdbc-derby";
    public static final String JDBC_H2 = "jdbc-h2";
    public static final String JDBC_POSTGRESQL = "jdbc-postgresql";
    public static final String JDBC_MARIADB = "jdbc-mariadb";
    public static final String JDBC_MSSQL = "jdbc-mssql";
    public static final String JDBC_MYSQL = "jdbc-mysql";
    public static final String JGIT = "jgit";
    public static final String JSCH = "jsch";
    public static final String KAFKA_STREAMS = "kafka-streams";
    public static final String KEYCLOAK_AUTHORIZATION = "keycloak-authorization";
    public static final String KOGITO = "kogito";
    public static final String KOTLIN = "kotlin";
    public static final String KUBERNETES = "kubernetes";
    public static final String KUBERNETES_CLIENT = "kubernetes-client";
    public static final String LOGGING_GELF = "logging-gelf";
    public static final String MAILER = "mailer";
    public static final String MONGODB_CLIENT = "mongodb-client";
    public static final String MONGODB_PANACHE = "mongodb-panache";
    public static final String NARAYANA_JTA = "narayana-jta";
    public static final String NARAYANA_STM = "narayana-stm";
    public static final String REACTIVE_PG_CLIENT = "reactive-pg-client";
    public static final String REACTIVE_MYSQL_CLIENT = "reactive-mysql-client";
    public static final String NEO4J = "neo4j";
    public static final String OIDC = "oidc";
    public static final String QUTE = "qute";
    public static final String RESTEASY = "resteasy";
    public static final String RESTEASY_JACKSON = "resteasy-jackson";
    public static final String RESTEASY_JAXB = "resteasy-jaxb";
    public static final String RESTEASY_JSONB = "resteasy-jsonb";
    public static final String RESTEASY_QUTE = "resteasy-qute";
    public static final String REST_CLIENT = "rest-client";
    public static final String SCALA = "scala";
    public static final String SCHEDULER = "scheduler";
    public static final String SECURITY = "security";
    public static final String SECURITY_JDBC = "security-jdbc";
    public static final String SECURITY_LDAP = "security-ldap";
    public static final String SECURITY_PROPERTIES_FILE = "security-properties-file";
    public static final String SECURITY_OAUTH2 = "security-oauth2";
    public static final String SERVLET = "servlet";
    public static final String SMALLRYE_CONTEXT_PROPAGATION = "smallrye-context-propagation";
    public static final String SMALLRYE_FAULT_TOLERANCE = "smallrye-fault-tolerance";
    public static final String SMALLRYE_HEALTH = "smallrye-health";
    public static final String SMALLRYE_JWT = "smallrye-jwt";
    public static final String SMALLRYE_METRICS = "smallrye-metrics";
    public static final String SMALLRYE_OPENAPI = "smallrye-openapi";
    public static final String SMALLRYE_OPENTRACING = "smallrye-opentracing";
    public static final String SMALLRYE_REACTIVE_MESSAGING = "smallrye-reactive-messaging";
    public static final String SMALLRYE_REACTIVE_MESSAGING_KAFKA = "smallrye-reactive-messaging-kafka";
    public static final String SMALLRYE_REACTIVE_MESSAGING_AMQP = "smallrye-reactive-messaging-amqp";
    public static final String SMALLRYE_REACTIVE_MESSAGING_MQTT = "smallrye-reactive-messaging-mqtt";
    public static final String SMALLRYE_REACTIVE_STREAMS_OPERATORS = "smallrye-reactive-streams-operators";
    public static final String SMALLRYE_REACTIVE_TYPE_CONVERTERS = "smallrye-reactive-type-converters";
    public static final String SPRING_DI = "spring-di";
    public static final String SPRING_WEB = "spring-web";
    public static final String SPRING_DATA_JPA = "spring-data-jpa";
    public static final String SPRING_SECURITY = "spring-security";
    public static final String SPRING_BOOT_PROPERTIES = "spring-boot-properties";
    public static final String SWAGGER_UI = "swagger-ui";
    public static final String TIKA = "tika";
    public static final String UNDERTOW_WEBSOCKETS = "undertow-websockets";
    public static final String VAULT = "vault";
    public static final String VERTX = "vertx";
    public static final String VERTX_WEB = "vertx-web";
    public static final String VERTX_GRAPHQL = "vertx-graphql";

    private final String info;

    public FeatureBuildItem(String info) {
        this.info = Objects.requireNonNull(info);
    }

    public String getInfo() {
        return info;
    }

}
