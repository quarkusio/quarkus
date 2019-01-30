package org.jboss.shamrock.agroal.runtime;

/**
 * Setting for SSL behaviour wrt server.
 */
public enum SslMode {
    /**
     * Keep default driver behaviour wrt SSL.
     */
    Default,
    
    /**
     * Prevent driver from using SSL.
     */
    Disable;
}
