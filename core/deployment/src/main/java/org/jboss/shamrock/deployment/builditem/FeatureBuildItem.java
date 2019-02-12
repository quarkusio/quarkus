package org.jboss.shamrock.deployment.builditem;

import java.util.Objects;

import org.jboss.builder.item.MultiBuildItem;

/**
 * Describes a functionality provided by an extension. The info is displayed to users.
 */
public final class FeatureBuildItem extends MultiBuildItem {

    public static final String AGROAL = "agroal";
    public static final String CDI = "cdi";
    public static final String TRANSACTIONS = "transactions";
    public static final String WEBSOCKET = "websocket";
    public static final String JAXRS = "jaxrs";
    public static final String JAXRS_JSON = "jaxrs-json";
    public static final String MP_HEALTH = "mp-health";
    public static final String MP_OPENAPI = "mp-openapi";
    public static final String MP_METRICS = "mp-metrics";
    public static final String MP_FAULT_TOLERANCE = "mp-fault-tolerance";
    public static final String MP_OPENTRACING = "mp-opentracing";
    public static final String MP_REST_CLIENT = "mp-rest-client";
    public static final String MP_REACTIVE_OPERATORS = "mp-reactive-streams-operators";
    public static final String MP_REACTIVE_MESSAGING = "mp-reactive-streams-messaging";
    public static final String MP_JWT = "mp-jwt";
    public static final String REACTIVE_CONVERTERS = "reactive-type-converters";
    public static final String HIBERNATE_VALIDATOR = "hibernate-validator";
    public static final String JPA = "jpa";
    public static final String JAEGER = "jaeger";
    public static final String SCHEDULER = "scheduler";
    public static final String SECURITY = "security";
    public static final String SPRING_DI = "spring-di";
    public static final String VERTX = "vertx";
    
    private final String info;
    
    public FeatureBuildItem(String info) {
        this.info = Objects.requireNonNull(info);
    }

    public String getInfo() {
        return info;
    }

}
