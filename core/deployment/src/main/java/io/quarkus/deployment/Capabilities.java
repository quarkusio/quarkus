package io.quarkus.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.builditem.CapabilityBuildItem;

/**
 * This build items holds the set of registered capabilities.
 * 
 * @see CapabilityBuildItem
 */
public final class Capabilities extends SimpleBuildItem {

    // The following constants will be removed at some point post Quarkus 1.6
    @Deprecated
    public static final String AGROAL = Capability.AGROAL.getName();
    @Deprecated
    public static final String CDI_ARC = Capability.CDI.getName();
    @Deprecated
    public static final String SERVLET = Capability.SERVLET.getName();
    @Deprecated
    public static final String TRANSACTIONS = Capability.TRANSACTIONS.getName();
    @Deprecated
    public static final String JACKSON = Capability.JACKSON.getName();
    @Deprecated
    public static final String JSONB = Capability.JSONB.getName();
    @Deprecated
    public static final String REST_JACKSON = Capability.REST_JACKSON.getName();
    @Deprecated
    public static final String REST_JSONB = Capability.REST_JSONB.getName();
    @Deprecated
    public static final String RESTEASY_JSON_EXTENSION = Capability.RESTEASY_JSON.getName();
    @Deprecated
    public static final String RESTEASY = Capability.RESTEASY.getName();
    @Deprecated
    public static final String JWT = Capability.JWT.getName();
    @Deprecated
    public static final String TIKA = Capability.TIKA.getName();
    @Deprecated
    public static final String MONGODB_PANACHE = Capability.MONGODB_PANACHE.getName();
    @Deprecated
    public static final String FLYWAY = Capability.FLYWAY.getName();
    @Deprecated
    public static final String LIQUIBASE = Capability.LIQUIBASE.getName();
    @Deprecated
    public static final String SECURITY = Capability.SECURITY.getName();
    @Deprecated
    public static final String SECURITY_ELYTRON_OAUTH2 = Capability.SECURITY_ELYTRON_OAUTH2.getName();
    @Deprecated
    public static final String SECURITY_ELYTRON_JDBC = Capability.SECURITY_ELYTRON_JDBC.getName();
    @Deprecated
    public static final String SECURITY_ELYTRON_LDAP = Capability.SECURITY_ELYTRON_LDAP.getName();
    @Deprecated
    public static final String SECURITY_JPA = Capability.SECURITY_JPA.getName();
    @Deprecated
    public static final String QUARTZ = Capability.QUARTZ.getName();
    @Deprecated
    public static final String METRICS = Capability.METRICS.getName();
    @Deprecated
    public static final String RESTEASY_MUTINY_EXTENSION = Capability.RESTEASY_MUTINY.getName();
    @Deprecated
    public static final String CONTAINER_IMAGE_JIB = Capability.CONTAINER_IMAGE_JIB.getName();
    @Deprecated
    public static final String CONTAINER_IMAGE_DOCKER = Capability.CONTAINER_IMAGE_DOCKER.getName();
    @Deprecated
    public static final String CONTAINER_IMAGE_S2I = Capability.CONTAINER_IMAGE_S2I.getName();
    @Deprecated
    public static final String CONTAINER_IMAGE_OPENSHIFT = Capability.CONTAINER_IMAGE_OPENSHIFT.getName();
    @Deprecated
    public static final String HIBERNATE_ORM = Capability.HIBERNATE_ORM.getName();
    @Deprecated
    public static final String SMALLRYE_OPENTRACING = Capability.SMALLRYE_OPENTRACING.getName();
    @Deprecated
    public static final String SPRING_WEB = Capability.SPRING_WEB.getName();

    private final Set<String> capabilities;

    public Capabilities(Set<String> capabilities) {
        this.capabilities = capabilities;
    }

    public Set<String> getCapabilities() {
        return capabilities;
    }

    public boolean isCapabilityPresent(String capability) {
        return isPresent(capability);
    }

    public boolean isPresent(Capability capability) {
        return isPresent(capability.getName());
    }

    public boolean isPresent(String capability) {
        return capabilities.contains(capability);
    }

    public boolean isMissing(String capability) {
        return !isPresent(capability);
    }

    public boolean isMissing(Capability capability) {
        return isMissing(capability.getName());
    }

}
