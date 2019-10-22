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
    public static final String JSONB = "io.quarkus.jsonb";
    public static final String RESTEASY_JSON_EXTENSION = "io.quarkus.resteasy-json";
    public static final String SECURITY = "io.quarkus.security";

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
