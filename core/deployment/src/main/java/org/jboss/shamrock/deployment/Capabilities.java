package org.jboss.shamrock.deployment;

import java.util.Set;

import org.jboss.builder.item.SimpleBuildItem;

/**
 * The list of capabilities.
 *
 */
public final class Capabilities extends SimpleBuildItem {

    public static final String CDI_WELD = "org.jboss.shamrock.cdi.weld";
    public static final String CDI_ARC = "org.jboss.shamrock.cdi.arc";
    public static final String TRANSACTIONS = "org.jboss.shamrock.transactions";


    private final Set<String> capabilities;

    public boolean isCapabilityPresent(String capability) {
        return capabilities.contains(capability);
    }

    public Capabilities(Set<String> capabilities) {
        this.capabilities = capabilities;
    }

}
