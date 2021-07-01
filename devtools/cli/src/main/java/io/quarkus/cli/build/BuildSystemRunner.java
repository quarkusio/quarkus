package io.quarkus.cli.build;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import io.quarkus.cli.common.BuildOptions;
import io.quarkus.cli.common.CategoryListFormatOptions;
import io.quarkus.cli.common.DebugOptions;
import io.quarkus.cli.common.DevOptions;
import io.quarkus.cli.common.ListFormatOptions;
import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.common.PropertiesOptions;
import io.quarkus.cli.common.RegistryClientMixin;
import io.quarkus.cli.common.RunModeOption;
import io.quarkus.devtools.project.BuildTool;
import picocli.CommandLine;

public interface BuildSystemRunner {

    static BuildSystemRunner getRunner(OutputOptionMixin output, PropertiesOptions propertiesOptions,
            RegistryClientMixin registryClient, Path projectRoot, BuildTool buildTool) {
        if (buildTool == null) {
            throw new IllegalStateException("Is this a project directory? Unable to find a build file in: " + projectRoot);
        }
        switch (buildTool) {
            default:
            case MAVEN:
                return new MavenRunner(output, propertiesOptions, projectRoot);
            case GRADLE_KOTLIN_DSL:
                return new GradleRunner(output, propertiesOptions, projectRoot, BuildTool.GRADLE_KOTLIN_DSL);
            case GRADLE:
                return new GradleRunner(output, propertiesOptions, projectRoot, BuildTool.GRADLE);
            case JBANG:
                return new JBangRunner(output, propertiesOptions, registryClient, projectRoot);
        }
    }

    default int run(BuildCommandArgs command) {
        try {
            return ExecuteUtil.executeProcess(getOutput(), command.arguments, command.targetDirectory);
        } catch (IOException | InterruptedException e) {
            getOutput().error("Command failed. " + e.getMessage());
            getOutput().printStackTrace(e);
            return CommandLine.ExitCode.SOFTWARE;
        }
    }

    default BuildCommandArgs prependExecutable(ArrayDeque<String> args) {
        BuildCommandArgs cmd = new BuildCommandArgs();
        File wrapper = getWrapper();
        if (wrapper != null) {
            args.addFirst(wrapper.getAbsolutePath());
            cmd.targetDirectory = wrapper.getParentFile();
        } else {
            File command = getExecutable();
            args.addFirst(command.getAbsolutePath());
            cmd.targetDirectory = getProjectRoot().toFile();
        }
        cmd.arguments = args.toArray(new String[0]);
        return cmd;
    }

    default List<String> flattenMappedProperties(Map<String, String> props) {
        List<String> result = new ArrayList<>();
        props.entrySet().forEach(x -> {
            if (x.getValue().length() > 0) {
                result.add("-D" + x.getKey() + "=" + x.getValue());
            } else {
                result.add("-D" + x.getKey());
            }
        });
        return result;
    }

    default String fixPath(Path absolutePath) {
        return getProjectRoot().relativize(absolutePath).toString();
    }

    Integer listExtensionCategories(RunModeOption runMode, CategoryListFormatOptions format)
            throws Exception;

    Integer listExtensions(RunModeOption runMode, ListFormatOptions format, boolean installable, String searchPattern,
            String category)
            throws Exception;

    Integer addExtension(RunModeOption runMode, Set<String> extensions) throws Exception;

    Integer removeExtension(RunModeOption runMode, Set<String> extensions) throws Exception;

    BuildCommandArgs prepareBuild(BuildOptions buildOptions, RunModeOption runMode, List<String> params);

    List<Supplier<BuildCommandArgs>> prepareDevMode(DevOptions devOptions, DebugOptions debugOptions,
            List<String> params);

    Path getProjectRoot();

    File getExecutable();

    File getWrapper();

    OutputOptionMixin getOutput();

    BuildTool getBuildTool();

    public static class BuildCommandArgs {
        String[] arguments;
        File targetDirectory;

        public String showCommand() {
            return String.join(" ", arguments);
        }

        @Override
        public String toString() {
            return "BuildCommandArgs{" +
                    "arguments=" + Arrays.toString(arguments) +
                    ", targetDirectory=" + targetDirectory +
                    '}';
        }
    }
}
