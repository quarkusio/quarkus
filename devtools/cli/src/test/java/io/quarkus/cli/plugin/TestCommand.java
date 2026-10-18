package io.quarkus.cli.plugin;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.cli.common.OutputOptionMixin;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command
public class TestCommand implements PluginCommand {

    private static final String DEFAULT_CMD = "cmd";

    private final String cmd;
    private final List<String> arguments = new ArrayList<>();

    public TestCommand() {
        this(DEFAULT_CMD);
    }

    public TestCommand(String cmd) {
        this.cmd = cmd;
    }

    @Override
    public Integer call() throws Exception {
        System.out.println("Calling " + cmd + " " + String.join(" ", arguments));
        return CommandLine.ExitCode.OK;
    }

    @Override
    public List<String> getCommand() {
        return List.of(cmd);
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
        return null;
    }
}
