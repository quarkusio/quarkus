package io.quarkus.deployment.pkg.steps;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.SystemUtils;

public class NativeImageBuildLocalRunner extends NativeImageBuildRunner {

    private final String nativeImageExecutable;
    private final File workingDirectory;

    public NativeImageBuildLocalRunner(String nativeImageExecutable, File workingDirectory) {
        this.nativeImageExecutable = nativeImageExecutable;
        this.workingDirectory = workingDirectory;
    }

    @Override
    protected String[] getGraalVMVersionCommand(List<String> args) {
        return buildCommand(args);
    }

    @Override
    protected String[] getBuildCommand(List<String> args) {
        return buildCommand(args);
    }

    @Override
    protected void objcopy(String... args) {
        final String[] command = new String[args.length + 1];
        command[0] = "objcopy";
        System.arraycopy(args, 0, command, 1, args.length);
        runCommand(command, null, workingDirectory);
    }

    @Override
    protected boolean objcopyExists() {
        if (!SystemUtils.IS_OS_LINUX) {
            return false;
        }

        // System path
        String systemPath = System.getenv("PATH");
        if (systemPath != null) {
            String[] pathDirs = systemPath.split(File.pathSeparator);
            for (String pathDir : pathDirs) {
                File dir = new File(pathDir);
                if (dir.isDirectory()) {
                    File file = new File(dir, "objcopy");
                    if (file.exists()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private String[] buildCommand(List<String> args) {
        return Stream.concat(Stream.of(nativeImageExecutable), args.stream()).toArray(String[]::new);
    }

}
