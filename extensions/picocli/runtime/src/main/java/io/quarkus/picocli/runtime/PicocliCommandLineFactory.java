package io.quarkus.picocli.runtime;

import picocli.CommandLine;

@FunctionalInterface
public interface PicocliCommandLineFactory {
    CommandLine create();
}
