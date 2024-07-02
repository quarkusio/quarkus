package io.quarkus.grpc.cli;

import java.util.concurrent.Callable;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@TopCommand
@Command(name = "grpc", sortOptions = false, header = "grpc CLI", subcommands = {
        ListCommand.class, DescribeCommand.class, InvokeCommand.class })
public class GrpcCommand implements Callable<Integer> {

    @Spec
    protected CommandSpec spec;

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "Display this help message.")
    public boolean help;

    @Override
    public Integer call() {
        CommandLine schemaCommand = spec.subcommands().get("list");
        return schemaCommand.execute();
    }
}