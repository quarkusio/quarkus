package io.quarkus.quickcli;

/**
 * Determines the scope of options defined in a command.
 */
public enum ScopeType {
    /** Options apply only to the command where they are defined. */
    LOCAL,
    /** Options are inherited by all subcommands. */
    INHERIT
}
