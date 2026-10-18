package io.quarkus.security.spi.runtime;

import jakarta.annotation.Priority;
import jakarta.interceptor.Interceptor;

public class SecurityHandlerConstants {

    /**
     * Invocation context data key used by the SecurityHandler to save a security checks state
     */
    public static final String SECURITY_HANDLER = "io.quarkus.security.securityHandler";

    /**
     * The SecurityHandler keep a state of security checks in the Invocation context data to prevent repeated checks.
     * `executed` means the check has already been done.
     */
    public static final String EXECUTED = "executed";

    /**
     * Interceptor priority for standard security interceptors.
     */
    public static final int SECURITY_INTERCEPTOR_PRIORITY = Interceptor.Priority.PLATFORM_BEFORE + 150;

    /**
     * Priority for interceptors propagating information indicating that a security check has been performed.
     */
    public static final int PREVENT_REPEATED_CHECKS_INTERCEPTOR_PRIORITY = SECURITY_INTERCEPTOR_PRIORITY - 10;
}
