package io.quarkus.cli.plugin;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.cli.common.OutputOptionMixin;
import picocli.CommandLine.Command;

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
        this.jbang = new JBangSupport(output);
        this.output = output;
        this.arguments.add(location);
    }

    @Override
    public List<String> getCommand() {
        return jbang.getCommand();
    }

    @Override
    public List<String> getArguments() {
        return arguments;
    }

    public OutputOptionMixin getOutput() {
        return output;
    }
}
