package io.quarkus.deployment;

/**
 * Represents a capability of a core extension.
 */
public interface Capability {

    String QUARKUS_PREFIX = "io.quarkus.";

    /**
     * A datasource connection pool implementation
     */
    String AGROAL = QUARKUS_PREFIX + "agroal";

    /**
     * JSR 365 compatible contexts and dependency injection
     */
    String CDI = QUARKUS_PREFIX + "cdi";

    String CONFIG_YAML = QUARKUS_PREFIX + "config.yaml";

    /**
     * Java Servlet API
     */
    String SERVLET = QUARKUS_PREFIX + "servlet";

    /**
     * Java Transaction API (JTA)
     */
    String TRANSACTIONS = QUARKUS_PREFIX + "transactions";

    String JACKSON = QUARKUS_PREFIX + "jackson";

    String JSONB = QUARKUS_PREFIX + "jsonb";

    String REST = QUARKUS_PREFIX + "rest";
    String REST_JACKSON = REST + ".jackson";
    String REST_JSONB = REST + ".jsonb";

    String RESTEASY = QUARKUS_PREFIX + "resteasy";
    String RESTEASY_JSON = RESTEASY + ".json";

    String RESTEASY_JSON_JACKSON = RESTEASY_JSON + ".jackson";
    String RESTEASY_JSON_JACKSON_CLIENT = RESTEASY_JSON_JACKSON + ".client";

    String RESTEASY_JSON_JSONB = RESTEASY_JSON + ".jsonb";
    String RESTEASY_JSON_JSONB_CLIENT = RESTEASY_JSON_JSONB + ".client";

    String RESTEASY_MUTINY = RESTEASY + ".mutiny";
    String RESTEASY_REACTIVE = RESTEASY + ".reactive";

    String JWT = QUARKUS_PREFIX + "jwt";

    String TIKA = QUARKUS_PREFIX + "tika";

    String MONGODB_PANACHE = QUARKUS_PREFIX + "mongodb.panache";
    String MONGODB_PANACHE_KOTLIN = MONGODB_PANACHE + ".kotlin";

    String ELASTICSEARCH_REST_HIGH_LEVEL_CLIENT = QUARKUS_PREFIX + "elasticsearch-rest-high-level-client";

    String FLYWAY = QUARKUS_PREFIX + "flyway";
    String LIQUIBASE = QUARKUS_PREFIX + "liquibase";

    String SECURITY = QUARKUS_PREFIX + "security";
    String SECURITY_ELYTRON_OAUTH2 = SECURITY + ".elytron.oauth2";
    String SECURITY_ELYTRON_JDBC = SECURITY + ".elytron.jdbc";
    String SECURITY_ELYTRON_LDAP = SECURITY + ".elytron.ldap";
    String SECURITY_JPA = SECURITY + ".jpa";

    String QUARTZ = QUARKUS_PREFIX + "quartz";
    String KUBERNETES_SERVICE_BINDING = QUARKUS_PREFIX + "kubernetes.service.binding";

    /**
     * @deprecated
     * @see io.quarkus.deployment.metrics.MetricsCapabilityBuildItem
     */
    String METRICS = QUARKUS_PREFIX + "metrics";
    String CONTAINER_IMAGE_JIB = QUARKUS_PREFIX + "container.image.jib";
    String CONTAINER_IMAGE_DOCKER = QUARKUS_PREFIX + "container.image.docker";
    String CONTAINER_IMAGE_S2I = QUARKUS_PREFIX + "container.image.s2i";
    String CONTAINER_IMAGE_OPENSHIFT = QUARKUS_PREFIX + "container.image.openshift";
    String HIBERNATE_ORM = QUARKUS_PREFIX + "hibernate.orm";
    String HIBERNATE_ENVERS = QUARKUS_PREFIX + "hibernate.envers";
    String HIBERNATE_REACTIVE = QUARKUS_PREFIX + "hibernate.reactive";
    String HIBERNATE_VALIDATOR = QUARKUS_PREFIX + "hibernate.validator";
    String OPENTELEMETRY_TRACER = QUARKUS_PREFIX + "opentelemetry.tracer";

    String OPENSHIFT = QUARKUS_PREFIX + "openshift";
    String OPENSHIFT_CLIENT = OPENSHIFT + ".client";

    /**
     * Presence of an io.opentracing tracer (for example, Jaeger).
     */
    String OPENTRACING = QUARKUS_PREFIX + "opentracing";
    /**
     * Presence of SmallRye OpenTracing.
     */
    String SMALLRYE_OPENTRACING = QUARKUS_PREFIX + "smallrye.opentracing";
    String SMALLRYE_HEALTH = QUARKUS_PREFIX + "smallrye.health";
    String SMALLRYE_OPENAPI = QUARKUS_PREFIX + "smallrye.openapi";
    String SMALLRYE_FAULT_TOLERANCE = QUARKUS_PREFIX + "smallrye.faulttolerance";

    String SPRING_WEB = QUARKUS_PREFIX + "spring.web";

    String VERTX = QUARKUS_PREFIX + "vertx";
    String VERTX_CORE = VERTX + ".core";
    String VERTX_HTTP = VERTX + ".http";

    String APICURIO_REGISTRY = QUARKUS_PREFIX + "apicurio.registry";
    String APICURIO_REGISTRY_AVRO = APICURIO_REGISTRY + ".avro";
}
