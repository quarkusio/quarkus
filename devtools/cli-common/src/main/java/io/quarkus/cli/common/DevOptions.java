package io.quarkus.cli.common;

import io.quarkus.quickcli.annotations.Option;

public class DevOptions {

    @Option(names = { "-B", "--batch-mode" }, description = "Run in non-interactive (batch) mode.")
    boolean batchMode;

    @Option(order = 2, names = { "--dry-run" }, description = "Show actions that would be taken.")
    boolean dryRun = false;

    // Allow the option variant, but don't crowd help
    @Option(names = { "--dryrun" }, hidden = true)
    boolean dryRun2 = false;

    @Option(order = 3, names = {
            "--clean" }, description = "Perform clean as part of build. False by default.", negatable = true)
    public boolean clean = false;

    // Invalid w/ continuous test mode. Leave for compat (hidden)
    @Option(order = 4, names = { "--no-tests" }, negatable = true, hidden = true)
    public boolean runTests = true;

    @Option(order = 5, names = { "--offline" }, description = "Work offline.", defaultValue = "false")
    public boolean offline = false;

    public boolean skipTests() {
        return !runTests;
    }

    public boolean isDryRun() {
        return dryRun || dryRun2;
    }

    public boolean isBatchMode() {
        return batchMode;
    }

    @Override
    public String toString() {
        return "DevOptions [clean=" + clean + ", tests=" + runTests + ", offline=" + offline +
                ", batch-mode=" + batchMode + ", dry-run=" + isDryRun() + "]";
    }
}
