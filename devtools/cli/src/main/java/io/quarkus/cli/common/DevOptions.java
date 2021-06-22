package io.quarkus.cli.common;

import picocli.CommandLine;

public class DevOptions {
    @CommandLine.Option(order = 2, names = { "--dry-run" }, description = "Show actions that would be taken.")
    boolean dryRun = false;

    // Allow the option variant, but don't crowd help
    @CommandLine.Option(names = { "--dryrun" }, hidden = true)
    boolean dryRun2 = false;

    @CommandLine.Option(order = 3, names = {
            "--clean" }, description = "Perform clean as part of build. False by default.", negatable = true)
    public boolean clean = false;

    @CommandLine.Option(order = 4, names = {
            "--no-tests" }, description = "Toggle continuous testing mode. Enabled by default.", negatable = true, hidden = true)
    public boolean runTests = true; // TODO: does this make sense re: continuous test?

    public boolean skipTests() {
        return !runTests;
    }

    public boolean isDryRun() {
        return dryRun || dryRun2;
    }

    @Override
    public String toString() {
        return "DevOptions [clean=" + clean + ", tests=" + runTests + "]";
    }
}
