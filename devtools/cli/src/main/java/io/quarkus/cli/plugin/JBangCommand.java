package io.quarkus.cli.plugin;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.cli.common.OutputOptionMixin;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;

@Command
public class JBangCommand implements PluginCommand {

    private String location; //alias, url, maven coords
    private JBangSupport jbang;
    private OutputOptionMixin output;
    private final List<String> arguments = new ArrayList<>();

    public JBangCommand() {
        super();
    }

    public JBangCommand(String location, OutputOptionMixin output) {
        this.location = location;
        this.jbang = new JBangSupport(!output.isCliTest(), output);
        this.output = output;
        this.arguments.add(location);
    }

    @Override
    public Integer call() throws Exception {
        if (jbang.ensureJBangIsInstalled()) {
            return PluginCommand.super.call();
        } else {
            output.error("Unable to find JBang! Command execution aborted!");
            return ExitCode.SOFTWARE;
        }
    }

    @Override
    public List<String> getCommand() {
        return jbang.getCommand();
    }

    @Override
    public List<String> getArguments() {
        return arguments;
    }

    @Override
    public OutputOptionMixin getOutput() {
        return output;
    }

    @Override
    public void useArguments(List<String> arguments) {
        this.arguments.clear();
        this.arguments.add(location);
        this.arguments.addAll(arguments);
    }
}
