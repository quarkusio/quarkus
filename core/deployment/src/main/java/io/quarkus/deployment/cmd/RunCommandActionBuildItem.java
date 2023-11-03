package io.quarkus.deployment.cmd;

import java.nio.file.Path;
import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

public final class RunCommandActionBuildItem extends MultiBuildItem {
    private final String commandName;
    private final List<String> args;
    private Path workingDirectory;
    private String startedExpression;
    private Path logFile;
    private boolean needsLogfile;

    public RunCommandActionBuildItem(String commandName, List<String> args, Path workingDirectory, String startedExpression,
            Path logFile,
            boolean needsLogfile) {
        this.args = args;
        this.commandName = commandName;
        this.workingDirectory = workingDirectory;
        this.startedExpression = startedExpression;
        this.logFile = logFile;
        this.needsLogfile = needsLogfile;
    }

    public String getCommandName() {
        return commandName;
    }

    public String getStartedExpression() {
        return startedExpression;
    }

    public Path getWorkingDirectory() {
        return workingDirectory;
    }

    public List<String> getArgs() {
        return args;
    }

    public Path getLogFile() {
        return logFile;
    }

    public boolean isNeedsLogfile() {
        return needsLogfile;
    }
}
