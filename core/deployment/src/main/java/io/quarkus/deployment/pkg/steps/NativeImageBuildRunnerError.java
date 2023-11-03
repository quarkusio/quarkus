package io.quarkus.deployment.pkg.steps;

import java.nio.file.Path;
import java.util.List;

public class NativeImageBuildRunnerError extends NativeImageBuildRunner {

    private String message;

    public NativeImageBuildRunnerError(String message) {
        this.message = message;
    }

    @Override
    public boolean isContainer() {
        return false;
    }

    @Override
    protected String[] getGraalVMVersionCommand(List<String> args) {
        throw new RuntimeException(message);
    }

    @Override
    protected String[] getBuildCommand(Path outputDir, List<String> args) {
        throw new RuntimeException(message);
    }

    @Override
    protected void objcopy(Path outputDir, String... args) {
        throw new RuntimeException(message);
    }
}
