package io.quarkus.cli.plugin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.cli.common.OutputOptionMixin;
import picocli.CommandLine.Command;

@Command
public class JarCommand implements PluginCommand {

    private JBangSupport jbang;
    private String name;
    private Path location; //May be path, url or CAGTV
    private List<String> arguments = new ArrayList<>();

    private OutputOptionMixin output;
    private Path workingDirectory;

    public JarCommand() {
    }

    public JarCommand(String name, Path location, OutputOptionMixin output, Path workingDirectory) {
        this.jbang = new JBangSupport(output.isCliTest(), output, workingDirectory);
        this.name = name;
        this.location = location;
        this.arguments = new ArrayList<>();
        this.output = output;
        this.workingDirectory = workingDirectory;
        arguments.add(location.toAbsolutePath().toString());
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
    public Path getWorkingDirectory() {
        return workingDirectory;
    }
}
