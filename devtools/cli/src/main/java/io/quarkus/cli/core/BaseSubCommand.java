package io.quarkus.cli.core;

import java.io.PrintWriter;

import io.quarkus.cli.QuarkusCli;
import picocli.CommandLine;

public abstract class BaseSubCommand {

    @CommandLine.Option(names = { "-B", "--batch-mode" }, order = 100, description = "run command in batch mode")
    protected boolean batchMode;

    @CommandLine.Option(names = { "-h", "--help" }, order = 101, usageHelp = true, description = "display this help message")
    protected boolean help;

    @CommandLine.ParentCommand
    protected QuarkusCli parent;

    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    public PrintWriter out() {
        return spec.commandLine().getOut();
    }

    public PrintWriter err() {
        return spec.commandLine().getErr();
    }

}
