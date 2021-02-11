package io.quarkus.cli.core;

import java.io.PrintWriter;

import io.quarkus.cli.QuarkusCli;
import picocli.CommandLine;

public abstract class BaseSubCommand {
    @CommandLine.Option(names = { "-h", "--help" }, order = 0, usageHelp = true, description = "display this help message")
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
