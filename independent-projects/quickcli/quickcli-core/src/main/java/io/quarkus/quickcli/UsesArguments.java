package io.quarkus.quickcli;

import java.util.List;

/**
 * Interface for dynamic commands (e.g. plugins) that accept unmatched arguments.
 * Implement this instead of relying on reflective method lookup.
 */
public interface UsesArguments {

    void useArguments(List<String> arguments);
}
