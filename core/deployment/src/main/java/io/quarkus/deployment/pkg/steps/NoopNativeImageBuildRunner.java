package io.quarkus.deployment.pkg.steps;

import java.nio.file.Path;
import java.util.List;

public class NoopNativeImageBuildRunner extends NativeImageBuildRunner {

    private static final String MESSAGE = "NoopNativeImageBuildRunner is not meant to be used to perform an actual build.";
    private final boolean isContainer;

    public NoopNativeImageBuildRunner(boolean isContainer) {
        this.isContainer = isContainer;
    }

    @Override
    public boolean isContainer() {
        return isContainer;
    }

    @Override
    protected String[] getGraalVMVersionCommand(List<String> args) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    protected String[] getBuildCommand(Path outputDir, List<String> args) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    protected void objcopy(Path outputDir, String... args) {
        throw new UnsupportedOperationException(MESSAGE);
    }
}
