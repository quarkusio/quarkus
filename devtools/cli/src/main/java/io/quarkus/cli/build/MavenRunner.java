package io.quarkus.cli.build;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import io.quarkus.cli.registry.RegistryClientMixin;
import io.quarkus.devtools.commands.AddExtensions;
import io.quarkus.devtools.commands.ListCategories;
import io.quarkus.devtools.commands.ListExtensions;
import io.quarkus.devtools.commands.RemoveExtensions;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.buildfile.MavenProjectBuildFile;
import picocli.CommandLine;

public class MavenRunner implements BuildSystemRunner {
    static final String[] windowsWrapper = { "mvnw.cmd", "mvnw.bat" };
    static final String otherWrapper = "mvnw";

    final OutputOptionMixin output;
    final RegistryClientMixin registryClient;
    final PropertiesOptions propertiesOptions;
    final Path projectRoot;

    public MavenRunner(OutputOptionMixin output, PropertiesOptions propertiesOptions, RegistryClientMixin registryClient,
            Path projectRoot) {
        this.output = output;
        this.projectRoot = projectRoot;
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
        return ExecuteUtil.findExecutable("mvn",
                "Unable to find the maven executable, is it in your path?",
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
        return BuildTool.MAVEN;
    }

    QuarkusProject quarkusProject() {
        return MavenProjectBuildFile.getProject(projectRoot, output, Version::clientVersion);
    }

    @Override
    public Integer listExtensionCategories(RunModeOption runMode, CategoryListFormatOptions format)
            throws Exception {

        QuarkusCommandOutcome outcome = new ListCategories(quarkusProject(), output)
                .fromCli(true)
                .format(format.getFormatString())
                .batchMode(runMode.isBatchMode())
                .execute();

        return outcome.isSuccess() ? CommandLine.ExitCode.OK : CommandLine.ExitCode.SOFTWARE;
    }

    @Override
    public Integer listExtensions(RunModeOption runMode, ListFormatOptions format, boolean installable, String searchPattern,
            String category)
            throws Exception {

        // we do not have to spawn to list extensions for maven
        QuarkusCommandOutcome outcome = new ListExtensions(quarkusProject(), output)
                .fromCli(true)
                .all(false)
                .installed(!installable)
                .format(format.getFormatString())
                .search(searchPattern)
                .category(category)
                .batchMode(runMode.isBatchMode())
                .execute();

        return outcome.isSuccess() ? CommandLine.ExitCode.OK : CommandLine.ExitCode.SOFTWARE;
    }

    @Override
    public Integer addExtension(RunModeOption runMode, Set<String> extensions) throws Exception {
        AddExtensions invoker = new AddExtensions(quarkusProject());
        invoker.extensions(extensions);
        return invoker.execute().isSuccess() ? CommandLine.ExitCode.OK : CommandLine.ExitCode.SOFTWARE;
    }

    @Override
    public Integer removeExtension(RunModeOption runMode, Set<String> extensions) throws Exception {
        RemoveExtensions invoker = new RemoveExtensions(quarkusProject());
        invoker.extensions(extensions);
        return invoker.execute().isSuccess() ? CommandLine.ExitCode.OK : CommandLine.ExitCode.SOFTWARE;
    }

    @Override
    public BuildCommandArgs prepareBuild(BuildOptions buildOptions, RunModeOption runMode, List<String> params) {
        ArrayDeque<String> args = new ArrayDeque<>();
        setMavenProperties(args, runMode.isBatchMode());

        if (runMode.isBatchMode()) {
            args.add("-B");
        }

        if (buildOptions.offline) {
            args.add("--offline");
        }

        if (buildOptions.clean) {
            args.add("clean");
        }
        args.add("install");

        if (buildOptions.buildNative) {
            args.add("-Dnative");
        }
        if (buildOptions.skipTests()) {
            setSkipTests(args);
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

        setMavenProperties(args, false);

        if (devOptions.clean) {
            args.add("clean");
        }
        args.add("quarkus:dev");

        if (devOptions.skipTests()) { // TODO: does this make sense?
            setSkipTests(args);
        }

        debugOptions.addDebugArguments(args, jvmArgs);
        propertiesOptions.flattenJvmArgs(jvmArgs, args);

        // Add any other unmatched arguments
        paramsToQuarkusArgs(params, args);

        BuildCommandArgs buildCommandArgs = prependExecutable(args);
        return Collections.singletonList(new Supplier<BuildCommandArgs>() {
            @Override
            public BuildCommandArgs get() {
                return buildCommandArgs;
            }
        });
    }

    void setSkipTests(ArrayDeque<String> args) {
        args.add("-DskipTests");
        args.add("-Dmaven.test.skip=true");
    }

    void setMavenProperties(ArrayDeque<String> args, boolean batchMode) {
        if (output.isShowErrors()) {
            args.addFirst("-e");
        }
        // batch mode typically disables ansi/color output
        if (output.isAnsiEnabled() && !batchMode) {
            args.addFirst("-Dstyle.color=always");
        }

        // add specified properties
        args.addAll(flattenMappedProperties(propertiesOptions.properties));
        args.add(registryClient.getRegistryClientProperty());
    }

    void verifyBuildFile() {
        File buildFile = projectRoot.resolve("pom.xml").toFile();
        if (!buildFile.isFile()) {
            throw new IllegalStateException("Is this a project directory? Unable to find a build file in: " + projectRoot);
        }
    }
}
