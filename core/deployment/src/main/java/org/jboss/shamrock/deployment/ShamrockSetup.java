package org.jboss.shamrock.deployment;

/**
 * Integration service that provides an entry point for framework providers to integrate with Shamrock.
 *
 *
 *
 */
public interface ShamrockSetup {

    void setup(SetupContext context);

}
