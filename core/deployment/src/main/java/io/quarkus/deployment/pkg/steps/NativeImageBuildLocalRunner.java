package io.quarkus.deployment.pkg.steps;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.SystemUtils;

import io.smallrye.common.process.ProcessUtil;

public class NativeImageBuildLocalRunner extends NativeImageBuildRunner {

    private final String nativeImageExecutable;

    public NativeImageBuildLocalRunner(String nativeImageExecutable) {
        this.nativeImageExecutable = nativeImageExecutable;
    }

    @Override
    public boolean isContainer() {
        return false;
    }

    @Override
    protected String[] getGraalVMVersionCommand(List<String> args) {
        return buildCommand(args);
    }

    @Override
    protected String[] getBuildCommand(Path outputDir, List<String> args) {
        return buildCommand(args);
    }

    @Override
    protected void objcopy(Path outputDir, String... args) {
        final String[] command = new String[args.length + 1];
        command[0] = "objcopy";
        System.arraycopy(args, 0, command, 1, args.length);
        runCommand(command, null, outputDir.toFile());
    }

    @Override
    protected boolean objcopyExists() {
        if (!SystemUtils.IS_OS_LINUX) {
            return false;
        }
        return ProcessUtil.pathOfCommand(Path.of("objcopy")).isPresent();
    }

    private String[] buildCommand(List<String> args) {
        return Stream.concat(Stream.of(nativeImageExecutable), args.stream()).toArray(String[]::new);
    }

}
