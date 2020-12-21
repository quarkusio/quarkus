package io.quarkus.deployment.pkg.steps;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import io.quarkus.deployment.util.ProcessUtil;

public class NativeImageBuildLocalRunner extends NativeImageBuildRunner {

    private final String nativeImageExecutable;

    public NativeImageBuildLocalRunner(String nativeImageExecutable) {
        this.nativeImageExecutable = nativeImageExecutable;
    }

    @Override
    public void cleanupServer(File outputDir, boolean processInheritIODisabled) throws InterruptedException, IOException {
        final ProcessBuilder pb = new ProcessBuilder(nativeImageExecutable, "--server-shutdown");
        pb.directory(outputDir);
        final Process process = ProcessUtil.launchProcess(pb, processInheritIODisabled);
        process.waitFor();
    }

    @Override
    protected String[] getGraalVMVersionCommand(List<String> args) {
        return buildCommand(args);
    }

    @Override
    protected String[] getBuildCommand(List<String> args) {
        return buildCommand(args);
    }

    private String[] buildCommand(List<String> args) {
        return Stream.concat(Stream.of(nativeImageExecutable), args.stream()).toArray(String[]::new);
    }

}
