package io.quarkus.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The list of capabilities.
 */
public final class Capabilities extends SimpleBuildItem {

    public static final String AGROAL = "io.quarkus.agroal";
    public static final String CDI_ARC = "io.quarkus.cdi";
    public static final String SERVLET = "io.quarkus.servlet";
    public static final String TRANSACTIONS = "io.quarkus.transactions";
    public static final String JACKSON = "io.quarkus.jackson";
    public static final String JSONB = "io.quarkus.jsonb";
    public static final String REST_JACKSON = "io.quarkus.rest.jackson";
    public static final String REST_JSONB = "io.quarkus.rest.jsonb";
    public static final String RESTEASY_JSON_EXTENSION = "io.quarkus.resteasy-json";
    public static final String RESTEASY = "io.quarkus.resteasy";
    public static final String JWT = "io.quarkus.jwt";
    public static final String TIKA = "io.quarkus.tika";
    public static final String MONGODB_PANACHE = "io.quarkus.mongodb.panache";
    public static final String FLYWAY = "io.quarkus.flyway";
    public static final String LIQUIBASE = "io.quarkus.liquibase";
    public static final String SECURITY = "io.quarkus.security";
    public static final String SECURITY_ELYTRON_OAUTH2 = "io.quarkus.elytron.security.oauth2";
    public static final String SECURITY_ELYTRON_JDBC = "io.quarkus.elytron.security.jdbc";
    public static final String SECURITY_ELYTRON_LDAP = "io.quarkus.elytron.security.ldap";
    public static final String SECURITY_JPA = "io.quarkus.security.jpa";
    public static final String QUARTZ = "io.quarkus.quartz";
    public static final String METRICS = "io.quarkus.metrics";
    public static final String RESTEASY_MUTINY_EXTENSION = "io.quarkus.resteasy-mutiny";
    public static final String CONTAINER_IMAGE_JIB = "io.quarkus.container-image-jib";
    public static final String CONTAINER_IMAGE_DOCKER = "io.quarkus.container-image-docker";
    public static final String CONTAINER_IMAGE_S2I = "io.quarkus.container-image-s2i";
    public static final String HIBERNATE_ORM = "io.quarkus.hibernate-orm";

    private final Set<String> capabilities;

    public boolean isCapabilityPresent(String capability) {
        return capabilities.contains(capability);
    }

    public Capabilities(Set<String> capabilities) {
        this.capabilities = capabilities;
    }

    public Set<String> getCapabilities() {
        return capabilities;
    }

}
