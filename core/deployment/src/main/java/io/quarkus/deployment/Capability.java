package io.quarkus.deployment;

/**
 * Represents a capability of a core extension.
 */
public enum Capability {

    /**
     * A datasource connection pool implementation
     */
    AGROAL,
    /**
     * JSR 365 compatible contexts and dependency injection
     */
    CDI,
    /**
     * Java Servlet API
     */
    SERVLET,
    /**
     * Java Transaction API (JTA)
     */
    TRANSACTIONS,
    JACKSON,
    JSONB,
    REST_JACKSON,
    REST_JSONB,
    RESTEASY,
    RESTEASY_JSON,
    RESTEASY_MUTINY,
    QUARKUS_REST,
    JWT,
    TIKA,
    MONGODB_PANACHE,
    MONGODB_PANACHE_KOTLIN,
    FLYWAY,
    LIQUIBASE,
    SECURITY,
    SECURITY_ELYTRON_OAUTH2,
    SECURITY_ELYTRON_JDBC,
    SECURITY_ELYTRON_LDAP,
    SECURITY_JPA,
    QUARTZ,
    /**
     * @deprecated
     * @see io.quarkus.deployment.metrics.MetricsCapabilityBuildItem
     */
    METRICS,
    CONTAINER_IMAGE_JIB,
    CONTAINER_IMAGE_DOCKER,
    CONTAINER_IMAGE_S2I,
    CONTAINER_IMAGE_OPENSHIFT,
    HIBERNATE_ORM,
    HIBERNATE_ENVERS,
    HIBERNATE_REACTIVE,
    HIBERNATE_VALIDATOR,
    /**
     * Presence of an io.opentracing tracer (for example, Jaeger).
     */
    OPENTRACING,
    /**
     * Presence of SmallRye OpenTracing.
     */
    SMALLRYE_OPENTRACING,
    SPRING_WEB,
    SMALLRYE_OPENAPI;

    /**
     *
     * @return the name
     */
    public String getName() {
        return "io.quarkus." + toString().toLowerCase().replace("_", ".");
    }

}
