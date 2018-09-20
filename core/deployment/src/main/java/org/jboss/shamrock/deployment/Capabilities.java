package org.jboss.shamrock.deployment;

/**
 * The list of capabilities.
 *
 *  @see ProcessorContext#isCapabilityPresent(String)
 *  @see SetupContext#addCapability(String)
 */
public final class Capabilities {

    public static final String CDI_WELD = "org.jboss.shamrock.cdi.weld";
    public static final String CDI_ARC = "org.jboss.shamrock.cdi.arc";

    private Capabilities() {
    }

}
