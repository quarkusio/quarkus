package io.quarkus.cli.common;

/**
 * Interface for CLI commands that provide OutputOptionMixin
 */
public interface OutputProvider {
    OutputOptionMixin getOutput();
}
