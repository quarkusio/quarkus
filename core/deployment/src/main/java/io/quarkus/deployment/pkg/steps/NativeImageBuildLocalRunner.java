package io.quarkus.deployment.pkg.steps;

import java.util.List;
import java.util.stream.Stream;

public class NativeImageBuildLocalRunner extends NativeImageBuildRunner {

    private final String nativeImageExecutable;

    public NativeImageBuildLocalRunner(String nativeImageExecutable) {
        this.nativeImageExecutable = nativeImageExecutable;
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
