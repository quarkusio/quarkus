package io.quarkus.cli.build;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.quarkus.cli.common.BuildOptions;
import io.quarkus.cli.common.CategoryListFormatOptions;
import io.quarkus.cli.common.DebugOptions;
import io.quarkus.cli.common.DevOptions;
import io.quarkus.cli.common.ListFormatOptions;
import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.common.PropertiesOptions;
import io.quarkus.cli.common.RunModeOption;
import io.quarkus.cli.registry.RegistryClientMixin;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.registry.config.RegistriesConfigLocator;

public class GradleRunner implements BuildSystemRunner {
    public static final String[] windowsWrapper = { "gradlew.cmd", "gradlew.bat" };
    public static final String otherWrapper = "gradlew";

    final OutputOptionMixin output;
    final RegistryClientMixin registryClient;
    final Path projectRoot;
    final BuildTool buildTool;
    final PropertiesOptions propertiesOptions;

    public GradleRunner(OutputOptionMixin output, PropertiesOptions propertiesOptions, RegistryClientMixin registryClient,
            Path projectRoot, BuildTool buildTool) {
        this.output = output;
        this.projectRoot = projectRoot;
        this.buildTool = buildTool;
        this.propertiesOptions = propertiesOptions;
        this.registryClient = registryClient;
        verifyBuildFile();
    }

    @Override
    public File getWrapper() {
        return ExecuteUtil.findWrapper(projectRoot, windowsWrapper, otherWrapper);
    }

    @Override
    public File getExecutable() {
        return ExecuteUtil.findExecutable("gradle",
                "Unable to find the gradle executable, is it in your path?",
                output);
    }

    @Override
    public Path getProjectRoot() {
        return projectRoot;
    }

    @Override
    public OutputOptionMixin getOutput() {
        return output;
    }

    @Override
    public BuildTool getBuildTool() {
        return buildTool;
    }

    @Override
    public Integer listExtensionCategories(RunModeOption runMode, CategoryListFormatOptions format) {
        ArrayDeque<String> args = new ArrayDeque<>();
        setGradleProperties(args, runMode.isBatchMode());

        args.add("listCategories");
        args.add("--fromCli");
        args.add("--format=" + format.getFormatString());
        return run(prependExecutable(args));
    }

    @Override
    public Integer listExtensions(RunModeOption runMode, ListFormatOptions format, boolean installable, String searchPattern,
            String category) {
        ArrayDeque<String> args = new ArrayDeque<>();
        setGradleProperties(args, runMode.isBatchMode());

        args.add("listExtensions");
        args.add("--fromCli");
        args.add("--format=" + format.getFormatString());
        if (category != null && !category.isBlank()) {
            args.add("--category=" + category);
        }
        if (!installable) {
            args.add("--installed");
        }
        if (searchPattern != null) {
            args.add("--searchPattern=" + searchPattern);
        }
        return run(prependExecutable(args));
    }

    @Override
    public Integer addExtension(RunModeOption runMode, Set<String> extensions) {
        ArrayDeque<String> args = new ArrayDeque<>();
        setGradleProperties(args, runMode.isBatchMode());

        args.add("addExtension");
        String param = "--extensions=" + String.join(",", extensions);
        args.add(param);
        return run(prependExecutable(args));
    }

    @Override
    public Integer removeExtension(RunModeOption runMode, Set<String> extensions) {
        ArrayDeque<String> args = new ArrayDeque<>();
        setGradleProperties(args, runMode.isBatchMode());

        args.add("removeExtension");
        String param = "--extensions=" + String.join(",", extensions);
        args.add(param);
        return run(prependExecutable(args));
    }

    @Override
    public BuildCommandArgs prepareBuild(BuildOptions buildOptions, RunModeOption runMode, List<String> params) {
        ArrayDeque<String> args = new ArrayDeque<>();
        setGradleProperties(args, runMode.isBatchMode());

        if (buildOptions.clean) {
            args.add("clean");
        }
        args.add("build");

        if (buildOptions.buildNative) {
            args.add("-Dquarkus.package.type=native");
        }
        if (buildOptions.skipTests()) {
            setSkipTests(args);
        }
        if (buildOptions.offline) {
            args.add("--offline");
        }

        // Add any other unmatched arguments
        args.addAll(params);

        return prependExecutable(args);
    }

    @Override
    public List<Supplier<BuildCommandArgs>> prepareDevMode(DevOptions devOptions, DebugOptions debugOptions,
            List<String> params) {
        ArrayDeque<String> args = new ArrayDeque<>();
        List<String> jvmArgs = new ArrayList<>();

        setGradleProperties(args, false);

        if (devOptions.clean) {
            args.add("clean");
        }
        args.add("quarkusDev");

        if (devOptions.skipTests()) { // TODO: does this make sense for dev mode?
            setSkipTests(args);
        }

        debugOptions.addDebugArguments(args, jvmArgs);
        propertiesOptions.flattenJvmArgs(jvmArgs, args);

        // Add any other unmatched arguments using quarkus.args
        paramsToQuarkusArgs(params, args);

        try {
            Path outputFile = Files.createTempFile("quarkus-dev", ".txt");
            args.add("-Dio.quarkus.devmode-args=" + outputFile.toAbsolutePath().toString());

            BuildCommandArgs buildCommandArgs = prependExecutable(args);
            return Arrays.asList(new Supplier<BuildCommandArgs>() {
                @Override
                public BuildCommandArgs get() {
                    return buildCommandArgs;
                }
            }, new Supplier<BuildCommandArgs>() {
                @Override
                public BuildCommandArgs get() {
                    try {
                        List<String> lines = Files.readAllLines(outputFile).stream().filter(s -> !s.isBlank())
                                .collect(Collectors.toList());
                        BuildCommandArgs cmd = new BuildCommandArgs();
                        cmd.arguments = lines.toArray(new String[0]);
                        cmd.targetDirectory = buildCommandArgs.targetDirectory;
                        return cmd;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void setSkipTests(ArrayDeque<String> args) {
        args.add("-x");
        args.add("test");
    }

    void setGradleProperties(ArrayDeque<String> args, boolean batchMode) {
        if (output.isShowErrors()) {
            args.add("--full-stacktrace");
        }
        // batch mode typically disables ansi/color output
        if (batchMode) {
            args.add("--console=plain");
        } else if (output.isAnsiEnabled()) {
            args.add("--console=rich");
        }
        if (output.isCliTest()) {
            // Make sure we stay where we should
            args.add("--project-dir=" + projectRoot.toAbsolutePath());
        }
        args.add(registryClient.getRegistryClientProperty());

        final String configFile = System.getProperty(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY);
        if (configFile != null) {
            args.add("-D" + RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY + "=" + configFile);
        }

        // add any other discovered properties
        args.addAll(flattenMappedProperties(propertiesOptions.properties));
    }

    void verifyBuildFile() {
        for (String buildFileName : buildTool.getBuildFiles()) {
            File buildFile = projectRoot.resolve(buildFileName).toFile();
            if (buildFile.exists()) {
                return;
            }
        }
        throw new IllegalStateException("Was not able to find a build file in: " + projectRoot
                + " based on the following list: " + String.join(",", buildTool.getBuildFiles()));
    }
}
