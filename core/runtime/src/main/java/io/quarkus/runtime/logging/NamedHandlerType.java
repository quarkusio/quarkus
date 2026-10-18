package io.quarkus.runtime.logging;

/**
 * The handler types supported for named logging handlers.
 */
public enum NamedHandlerType {
    CONSOLE,
    FILE,
    SYSLOG,
    SOCKET
}
