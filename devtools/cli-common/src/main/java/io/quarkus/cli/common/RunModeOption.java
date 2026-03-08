package io.quarkus.cli.common;

import io.quarkus.quickcli.annotations.Option;

public class RunModeOption {

    @Option(names = { "-B",
            "--batch-mode" }, description = "Run in non-interactive (batch) mode.")
    boolean batchMode;

    // Allow the option variant, but don't crowd help
    @Option(names = { "--dryrun" }, hidden = true)
    boolean dryRun2 = false;

    @Option(names = { "--dry-run" }, description = "Show actions that would be taken.")
    boolean dryRun = false;

    public boolean isBatchMode() {
        return batchMode;
    }

    public boolean isDryRun() {
        return dryRun || dryRun2;
    }

    @Override
    public String toString() {
        return "RunModeOption [batchMode=" + batchMode + ", dryRun=" + isDryRun() + "]";
    }

}
