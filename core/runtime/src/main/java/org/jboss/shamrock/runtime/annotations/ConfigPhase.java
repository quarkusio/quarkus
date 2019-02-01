package org.jboss.shamrock.runtime.annotations;

public enum ConfigPhase {
    /**
     * Values are available for usage at build time.
     */
    BUILD,
    /**
     * Values are loaded during static initialization and available for usage at run time.
     */
    STATIC_INIT,
    /**
     * Values are available for usage at run time.
     */
    MAIN,
}
