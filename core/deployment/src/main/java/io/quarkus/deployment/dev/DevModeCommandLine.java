package io.quarkus.deployment.dev;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DevModeCommandLine {

    public static DevModeCommandLineBuilder builder(String java) {
        return new DevModeCommandLineBuilder(java);
    }

    private final List<String> args;
    private final String debugPort;
    private final Collection<Path> buildFiles;
    private final Map<String, String> launcherEnvironmentVariables;

    public DevModeCommandLine(List<String> args, String debugPort, Collection<Path> buildFiles,
            Map<String, String> launcherEnvironmentVariables) {
        this.args = args;
        this.debugPort = debugPort;
        this.buildFiles = buildFiles;
        this.launcherEnvironmentVariables = launcherEnvironmentVariables;
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

    public Map<String, String> getLauncherEnvironmentVariables() {
        return launcherEnvironmentVariables;
    }

}
