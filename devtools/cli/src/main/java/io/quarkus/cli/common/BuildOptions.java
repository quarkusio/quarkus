package io.quarkus.cli.common;

import picocli.CommandLine;

public class BuildOptions {
    @CommandLine.Option(order = 3, names = {
            "--clean" }, description = "Perform clean as part of build. False by default.", negatable = true)
    public boolean clean = false;

    @CommandLine.Option(order = 4, names = { "--native" }, description = "Build native executable.", defaultValue = "false")
    public boolean buildNative = false;

    @CommandLine.Option(order = 5, names = { "--offline" }, description = "Work offline.", defaultValue = "false")
    public boolean offline = false;

    @CommandLine.Option(order = 6, names = { "--skip-tests" }, description = "Skip tests.")
    public boolean runTests = true;

    public boolean skipTests() {
        return !runTests;
    }
}
