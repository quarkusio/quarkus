package io.quarkus.deployment.builditem;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Describes a functionality provided by an extension. The info is displayed to users.
 */
public final class FeatureBuildItem extends MultiBuildItem {

    public static final String AGROAL = "agroal";
    public static final String KUBERNETES = "kubernetes";
    public static final String CAMEL_CORE = "camel-core";
    public static final String CAMEL_INFINISPAN = "camel-infinispan";
    public static final String CAMEL_AWS_S3 = "camel-aws-s3";
    public static final String CAMEL_NETTY4_HTTP = "camel-netty4-http";
    public static final String CAMEL_SALESFORCE = "camel-salesforce";
    public static final String CDI = "cdi";
    public static final String ELASTICSEARCH_REST_CLIENT = "elasticsearch-rest-client";
    public static final String FLYWAY = "flyway";
    public static final String HIBERNATE_ORM = "hibernate-orm";
    public static final String HIBERNATE_VALIDATOR = "hibernate-validator";
    public static final String HIBERNATE_SEARCH_ELASTICSEARCH = "hibernate-search-elasticsearch";
    public static final String INFINISPAN_CLIENT = "infinispan-client";
    public static final String JAEGER = "jaeger";
    public static final String JDBC_H2 = "jdbc-h2";
    public static final String JDBC_MARIADB = "jdbc-mariadb";
    public static final String JDBC_POSTGRESQL = "jdbc-postgresql";
    public static final String JDBC_MSSQL = "jdbc-mssql";
    public static final String KEYCLOAK = "keycloak";
    public static final String KOTLIN = "kotlin";
    public static final String NARAYANA_JTA = "narayana-jta";
    public static final String REACTIVE_PG_CLIENT = "reactive-pg-client";
    public static final String RESTEASY = "resteasy";
    public static final String RESTEASY_JSONB = "resteasy-jsonb";
    public static final String SCHEDULER = "scheduler";
    public static final String SECURITY = "security";
    public static final String SMALLRYE_CONTEXT_PROPAGATION = "smallrye-context-propagation";
    public static final String SMALLRYE_FAULT_TOLERANCE = "smallrye-fault-tolerance";
    public static final String SMALLRYE_HEALTH = "smallrye-health";
    public static final String SMALLRYE_JWT = "smallrye-jwt";
    public static final String SMALLRYE_METRICS = "smallrye-metrics";
    public static final String SMALLRYE_OPENAPI = "smallrye-openapi";
    public static final String SMALLRYE_OPENTRACING = "smallrye-opentracing";
    public static final String SMALLRYE_REACTIVE_MESSAGING = "smallrye-reactive-messaging";
    public static final String SMALLRYE_REACTIVE_MESSAGING_KAFKA = "smallrye-reactive-messaging-kafka";
    public static final String SMALLRYE_REACTIVE_STREAMS_OPERATORS = "smallrye-reactive-streams-operators";
    public static final String SMALLRYE_REACTIVE_TYPE_CONVERTERS = "smallrye-reactive-type-converters";
    public static final String SMALLRYE_REST_CLIENT = "smallrye-rest-client";
    public static final String SPRING_DI = "spring-di";
    public static final String SWAGGER_UI = "swagger-ui";
    public static final String UNDERTOW_WEBSOCKETS = "undertow-websockets";
    public static final String VERTX = "vertx";
    public static final String VERTX_WEB = "vertx-web";

    private final String info;

    public FeatureBuildItem(String info) {
        this.info = Objects.requireNonNull(info);
    }

    public String getInfo() {
        return info;
    }

}
