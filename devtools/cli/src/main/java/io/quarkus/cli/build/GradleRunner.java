package io.quarkus.cli.build;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;

import io.quarkus.cli.common.BuildOptions;
import io.quarkus.cli.common.CategoryListFormatOptions;
import io.quarkus.cli.common.DebugOptions;
import io.quarkus.cli.common.DevOptions;
import io.quarkus.cli.common.ListFormatOptions;
import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.common.PropertiesOptions;
import io.quarkus.cli.common.RunModeOption;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.registry.config.RegistriesConfigLocator;

public class GradleRunner implements BuildSystemRunner {
    public static final String[] windowsWrapper = { "gradlew.cmd", "gradlew.bat" };
    public static final String otherWrapper = "gradlew";

    final OutputOptionMixin output;
    final Path projectRoot;
    final BuildTool buildTool;

    public GradleRunner(OutputOptionMixin output, Path projectRoot, BuildTool buildTool) {
        this.output = output;
        this.projectRoot = projectRoot;
        this.buildTool = buildTool;
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
    public BuildCommandArgs prepareBuild(BuildOptions buildOptions, PropertiesOptions propertiesOptions, RunModeOption runMode,
            List<String> params) {
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

        // add any other discovered properties
        args.addAll(flattenMappedProperties(propertiesOptions.properties));
        // Add any other unmatched arguments
        args.addAll(params);

        return prependExecutable(args);
    }

    @Override
    public BuildCommandArgs prepareDevMode(DevOptions devOptions, PropertiesOptions propertiesOptions,
            DebugOptions debugOptions, List<String> params) {
        ArrayDeque<String> args = new ArrayDeque<>();
        setGradleProperties(args, false);

        if (devOptions.clean) {
            args.addFirst("clean");
        }
        args.addFirst("quarkusDev");

        if (devOptions.skipTests()) { // TODO: does this make sense?
            setSkipTests(args);
        }

        //TODO: addDebugArguments(args, debugOptions);

        // add any other discovered properties
        args.addAll(flattenMappedProperties(propertiesOptions.properties));
        // Add any other unmatched arguments
        args.addAll(params);
        return prependExecutable(args);
    }

    void setSkipTests(ArrayDeque<String> args) {
        args.add("-x");
        args.add("test");
    }

    void setGradleProperties(ArrayDeque<String> args, boolean batchMode) {
        if (output.isShowErrors()) {
            args.addFirst("--full-stacktrace");
        }
        // batch mode typically disables ansi/color output
        if (batchMode) {
            args.addFirst("--console=plain");
        } else if (output.isAnsiEnabled()) {
            args.addFirst("--console=rich");
        }
        args.add("--project-dir=" + projectRoot.toAbsolutePath());
        ExecuteUtil.propagatePropertyIfSet("maven.repo.local", args);
        ExecuteUtil.propagatePropertyIfSet(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY, args);
        ExecuteUtil.propagatePropertyIfSet("io.quarkus.maven.secondary-local-repo", args);
    }

    void verifyBuildFile() {
        File buildFile = projectRoot.resolve("build.gradle").toFile();
        if (!buildFile.isFile()) {
            throw new IllegalStateException("Was not able to find a build file in: " + projectRoot);
        }
    }
}
