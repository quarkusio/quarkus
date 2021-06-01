package io.quarkus.cli.common;

import picocli.CommandLine;

public class RunModeOption {

    @CommandLine.Option(names = { "-B",
            "--batch-mode" }, description = "Run in non-interactive (batch) mode.")
    boolean batchMode;

    @CommandLine.Option(names = { "--dryrun" }, description = "Show actions that would be taken.")
    boolean dryRun = false;

    public boolean isBatchMode() {
        return batchMode;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    @Override
    public String toString() {
        return "RunModeOption [batchMode=" + batchMode + ", dryRun=" + dryRun + "]";
    }

}
