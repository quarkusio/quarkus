package io.quarkus.quickcli.runtime;

import io.quarkus.quickcli.CommandLine;

/**
 * Factory for creating QuickCLI CommandLine instances.
 * Can be overridden by providing a custom CDI bean.
 */
@FunctionalInterface
public interface QuickCliCommandLineFactory {
    CommandLine create();
}
