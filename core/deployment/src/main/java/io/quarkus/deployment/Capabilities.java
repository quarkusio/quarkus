package io.quarkus.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The list of capabilities.
 */
public final class Capabilities extends SimpleBuildItem {

    public static final String CDI_ARC = "io.quarkus.cdi";
    public static final String SERVLET = "io.quarkus.servlet";
    public static final String TRANSACTIONS = "io.quarkus.transactions";
    public static final String JACKSON = "io.quarkus.jackson";
    public static final String JSONB = "io.quarkus.jsonb";
    public static final String RESTEASY_JSON_EXTENSION = "io.quarkus.resteasy-json";
    public static final String RESTEASY = "io.quarkus.resteasy";
    public static final String JWT = "io.quarkus.jwt";
    public static final String TIKA = "io.quarkus.tika";
    public static final String MONGODB_PANACHE = "io.quarkus.mongodb.panache";
    public static final String KOGITO = "io.quarkus.kogito";
    public static final String FLYWAY = "io.quarkus.flyway";
    public static final String SECURITY = "io.quarkus.security";
    public static final String SECURITY_ELYTRON_OAUTH2 = "io.quarkus.elytron.security.oauth2";
    public static final String SECURITY_ELYTRON_JDBC = "io.quarkus.elytron.security.jdbc";
    public static final String SECURITY_ELYTRON_LDAP = "io.quarkus.elytron.security.ldap";
    public static final String QUARTZ = "io.quarkus.quartz";
    public static final String METRICS = "io.quarkus.metrics";

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
