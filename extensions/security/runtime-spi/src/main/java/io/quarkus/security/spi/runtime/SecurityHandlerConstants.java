package io.quarkus.security.spi.runtime;

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
}
