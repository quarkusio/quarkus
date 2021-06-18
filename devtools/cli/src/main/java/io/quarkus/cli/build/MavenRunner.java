package io.quarkus.cli.build;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;

import io.quarkus.cli.Version;
import io.quarkus.cli.common.BuildOptions;
import io.quarkus.cli.common.DebugOptions;
import io.quarkus.cli.common.DevOptions;
import io.quarkus.cli.common.ListFormatOptions;
import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.common.PropertiesOptions;
import io.quarkus.cli.common.RunModeOption;
import io.quarkus.devtools.commands.AddExtensions;
import io.quarkus.devtools.commands.ListExtensions;
import io.quarkus.devtools.commands.RemoveExtensions;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.buildfile.MavenProjectBuildFile;
import io.quarkus.registry.config.RegistriesConfigLocator;
import picocli.CommandLine;

public class MavenRunner implements BuildSystemRunner {
    static final String[] windowsWrapper = { "mvnw.cmd", "mvnw.bat" };
    static final String otherWrapper = "mvnw";

    final OutputOptionMixin output;
    final Path projectRoot;

    public MavenRunner(OutputOptionMixin output, Path projectRoot) {
        this.output = output;
        this.projectRoot = projectRoot;
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
    public Integer listExtensions(RunModeOption runMode, ListFormatOptions format, boolean installable, String searchPattern)
            throws Exception {
        // we do not have to spawn to list extensions for maven
        QuarkusCommandOutcome outcome = new ListExtensions(quarkusProject(), output)
                .fromCli(true)
                .all(false)
                .installed(!installable)
                .format(format.getFormatString())
                .search(searchPattern)
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
    public BuildCommandArgs prepareBuild(BuildOptions buildOptions, PropertiesOptions propertiesOptions, RunModeOption runMode,
            List<String> params) {
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
        setMavenProperties(args, false);

        if (devOptions.clean) {
            args.add("clean");
        }
        args.add("quarkus:dev");

        if (devOptions.skipTests()) { // TODO: does this make sense?
            setSkipTests(args);
        }

        //TODO: addDebugArguments(args, debugOptions);

        args.addAll(flattenMappedProperties(propertiesOptions.properties));
        // Add any other unmatched arguments
        args.addAll(params);
        return prependExecutable(args);
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
        ExecuteUtil.propagatePropertyIfSet("maven.repo.local", args);
        ExecuteUtil.propagatePropertyIfSet(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY, args);
        ExecuteUtil.propagatePropertyIfSet("io.quarkus.maven.secondary-local-repo", args);
    }

    void verifyBuildFile() {
        File buildFile = projectRoot.resolve("pom.xml").toFile();
        if (!buildFile.isFile()) {
            throw new IllegalStateException("Is this a project directory? Unable to find a build file in: " + projectRoot);
        }
    }
}
