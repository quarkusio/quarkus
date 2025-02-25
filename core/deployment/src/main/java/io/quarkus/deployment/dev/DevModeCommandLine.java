package io.quarkus.deployment.dev;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class DevModeCommandLine {

    public static DevModeCommandLineBuilder builder(String java) {
        return new DevModeCommandLineBuilder(java);
    }

    private final List<String> args;
    private final String debugPort;
    private final Collection<Path> buildFiles;

    public DevModeCommandLine(List<String> args, String debugPort, Collection<Path> buildFiles) {
        this.args = args;
        this.debugPort = debugPort;
        this.buildFiles = buildFiles;
    }

    public List<String> getArguments() {
        return args;
    }

    public Collection<Path> getWatchedBuildFiles() {
        return buildFiles;
    }

    public String getDebugPort() {
        return debugPort;
    }

}
