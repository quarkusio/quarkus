package io.quarkus.cli.plugin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.quarkus.cli.common.OutputOptionMixin;
import picocli.CommandLine.Command;

@Command
public class ShellCommand implements PluginCommand, Callable<Integer> {

    private String name;
    private Path command;
    private OutputOptionMixin output;

    private final List<String> arguments = new ArrayList<>();

    public ShellCommand() {
    }

    public ShellCommand(String name, Path command, OutputOptionMixin output) {
        this.name = name;
        this.command = command;
        this.output = output;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public List<String> getCommand() {
        return List.of(command.toString());
    }

    @Override
    public List<String> getArguments() {
        return arguments;
    }

    @Override
    public void useArguments(List<String> arguments) {
        this.arguments.clear();
        this.arguments.addAll(arguments);
    }

    @Override
    public OutputOptionMixin getOutput() {
        return output;
    }
}
