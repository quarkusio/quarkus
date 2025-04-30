package io.quarkus.cli.build;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;

import io.quarkus.cli.Version;
import io.quarkus.cli.common.BuildOptions;
import io.quarkus.cli.common.CategoryListFormatOptions;
import io.quarkus.cli.common.DebugOptions;
import io.quarkus.cli.common.DevOptions;
import io.quarkus.cli.common.ListFormatOptions;
import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.common.PropertiesOptions;
import io.quarkus.cli.common.RunModeOption;
import io.quarkus.cli.common.TargetQuarkusVersionGroup;
import io.quarkus.cli.registry.RegistryClientMixin;
import io.quarkus.cli.update.RewriteGroup;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.platform.tools.ToolsUtils;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.config.RegistriesConfigLocator;
import io.quarkus.runtime.util.StringUtil;

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
    public Integer projectInfo(boolean perModule) {
        ArrayDeque<String> args = new ArrayDeque<>();
        args.add("quarkusInfo");
        if (perModule) {
            args.add("--per-module");
        }
        return run(prependExecutable(args));
    }

    @Override
    public Integer updateProject(TargetQuarkusVersionGroup targetQuarkusVersion, RewriteGroup rewrite)
            throws Exception {
        final ExtensionCatalog extensionCatalog = ToolsUtils.resolvePlatformDescriptorDirectly(
                ToolsConstants.QUARKUS_CORE_GROUP_ID, null,
                Version.clientVersion(),
                QuarkusProjectHelper.artifactResolver(), MessageWriter.info());
        final Properties props = ToolsUtils.readQuarkusProperties(extensionCatalog);
        ArrayDeque<String> args = new ArrayDeque<>();
        args.add("-PquarkusPluginVersion=" + ToolsUtils.getGradlePluginVersion(props));
        args.add("--console");
        args.add("plain");
        args.add("--no-daemon");
        args.add("--stacktrace");
        args.add("quarkusUpdate");
        if (!StringUtil.isNullOrEmpty(targetQuarkusVersion.platformVersion)) {
            args.add("--platformVersion");
            args.add(targetQuarkusVersion.platformVersion);
        }
        if (!StringUtil.isNullOrEmpty(targetQuarkusVersion.streamId)) {
            args.add("--stream");
            args.add(targetQuarkusVersion.streamId);
        }
        if (rewrite.pluginVersion != null) {
            args.add("--rewritePluginVersion=" + rewrite.pluginVersion);
        }
        if (rewrite.quarkusUpdateRecipes != null) {
            args.add("--quarkusUpdateRecipes=" + rewrite.quarkusUpdateRecipes);
        }
        if (rewrite.additionalUpdateRecipes != null) {
            args.add("--additionalUpdateRecipes=" + rewrite.additionalUpdateRecipes);
        }
        if (rewrite.run != null) {
            if (rewrite.run.yes) {
                args.add("--rewrite");
            }
            if (rewrite.run.no) {
                args.add("--rewrite=false");
            }
            if (rewrite.run.dryRun) {
                args.add("--rewriteDryRun");
            }
        }
        return run(prependExecutable(args));

    }

    @Override
    public BuildCommandArgs prepareBuild(BuildOptions buildOptions, RunModeOption runMode, List<String> params) {
        return prepareAction("build", buildOptions, runMode, params);
    }

    @Override
    public BuildCommandArgs prepareAction(String action, BuildOptions buildOptions, RunModeOption runMode,
            List<String> params) {
        ArrayDeque<String> args = new ArrayDeque<>();
        setGradleProperties(args, runMode.isBatchMode());

        if (buildOptions.clean) {
            args.add("clean");
        }
        args.add(action);

        if (buildOptions.buildNative) {
            args.add("-Dquarkus.native.enabled=true");
            args.add("-Dquarkus.package.jar.enabled=false");
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
    public BuildCommandArgs prepareTest(BuildOptions buildOptions, RunModeOption runMode, List<String> params, String filter) {
        if (filter != null) {
            params.add("--tests " + filter);
        }
        return prepareAction("test", buildOptions, runMode, params);
    }

    @Override
    public List<Supplier<BuildCommandArgs>> prepareDevTestMode(boolean devMode, DevOptions commonOptions,
            DebugOptions debugOptions, List<String> params) {
        ArrayDeque<String> args = new ArrayDeque<>();
        List<String> jvmArgs = new ArrayList<>();

        setGradleProperties(args, commonOptions.isBatchMode());

        if (commonOptions.clean) {
            args.add("clean");
        }

        args.add(devMode ? "quarkusDev" : "quarkusTest");

        if (commonOptions.offline) {
            args.add("--offline");
        }

        debugOptions.addDebugArguments(args, jvmArgs);
        propertiesOptions.flattenJvmArgs(jvmArgs, args);

        // Add any other unmatched arguments using quarkus.args
        paramsToQuarkusArgs(params, args);

        try {
            Path outputFile = Files.createTempFile("quarkus-dev", ".txt");
            if (devMode) {
                args.add("-Dio.quarkus.devmode-args=" + outputFile.toAbsolutePath());
            }

            BuildCommandArgs buildCommandArgs = prependExecutable(args);
            return Arrays.asList(() -> buildCommandArgs, () -> {
                try {
                    BuildCommandArgs cmd = new BuildCommandArgs();
                    cmd.arguments = Files.readAllLines(outputFile).stream().filter(s -> !s.isBlank()).toArray(String[]::new);
                    cmd.targetDirectory = buildCommandArgs.targetDirectory;
                    return cmd;
                } catch (IOException e) {
                    throw new RuntimeException(e);
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

        final String configFile = registryClient.getConfigArg() == null
                ? System.getProperty(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY)
                : registryClient.getConfigArg();
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
