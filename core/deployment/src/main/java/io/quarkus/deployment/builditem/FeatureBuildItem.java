package io.quarkus.deployment.builditem;

import java.util.Objects;

import org.jboss.builder.item.MultiBuildItem;

/**
 * Describes a functionality provided by an extension. The info is displayed to users.
 */
public final class FeatureBuildItem extends MultiBuildItem {

    public static final String AGROAL = "agroal";
    public static final String CAMEL_CORE = "camel-core";
    public static final String CAMEL_INFINISPAN = "camel-infinispan";
    public static final String CAMEL_NETTY4_HTTP = "camel-netty4-http";
    public static final String CAMEL_SALESFORCE = "camel-salesforce";
    public static final String CDI = "cdi";
    public static final String HIBERNATE_ORM = "hibernate-orm";
    public static final String HIBERNATE_VALIDATOR = "hibernate-validator";
    public static final String JAEGER = "jaeger";
    public static final String NARAYANA_JTA = "narayana-jta";
    public static final String RESTEASY = "resteasy";
    public static final String RESTEASY_JSONB = "resteasy-jsonb";
    public static final String SCHEDULER = "scheduler";
    public static final String SECURITY = "security";
    public static final String SMALLRYE_HEALTH = "smallrye-health";
    public static final String SMALLRYE_OPENAPI = "smallrye-openapi";
    public static final String SMALLRYE_METRICS = "smallrye-metrics";
    public static final String SMALLRYE_FAULT_TOLERANCE = "smallrye-fault-tolerance";
    public static final String SMALLRYE_JWT = "smallrye-jwt";
    public static final String SMALLRYE_OPENTRACING = "smallrye-opentracing";
    public static final String SMALLRYE_REACTIVE_MESSAGING = "smallrye-reactive-messaging";
    public static final String SMALLRYE_REACTIVE_STREAMS_OPERATORS = "smallrye-reactive-streams-operators";
    public static final String SMALLRYE_REACTIVE_TYPE_CONVERTERS = "smallrye-reactive-type-converters";
    public static final String SMALLRYE_REST_CLIENT = "smallrye-rest-client";
    public static final String SPRING_DI = "spring-di";
    public static final String UNDERTOW_WEBSOCKETS = "undertow-websockets";
    public static final String VERTX = "vertx";

    private final String info;

    public FeatureBuildItem(String info) {
        this.info = Objects.requireNonNull(info);
    }

    public String getInfo() {
        return info;
    }

}