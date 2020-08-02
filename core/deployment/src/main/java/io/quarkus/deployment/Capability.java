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
    JWT,
    TIKA,
    MONGODB_PANACHE,
    FLYWAY,
    LIQUIBASE,
    SECURITY,
    SECURITY_ELYTRON_OAUTH2,
    SECURITY_ELYTRON_JDBC,
    SECURITY_ELYTRON_LDAP,
    SECURITY_JPA,
    QUARTZ,
    METRICS,
    CONTAINER_IMAGE_JIB,
    CONTAINER_IMAGE_DOCKER,
    CONTAINER_IMAGE_S2I,
    HIBERNATE_ORM,
    HIBERNATE_REACTIVE,
    SMALLRYE_OPENTRACING,
    SPRING_WEB;

    /**
     * 
     * @return the name
     */
    public String getName() {
        return "io.quarkus." + toString().toLowerCase().replace("_", ".");
    }

}
