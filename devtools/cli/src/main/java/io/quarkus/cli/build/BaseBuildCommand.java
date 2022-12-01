package io.quarkus.cli.build;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.cli.common.HelpOption;
import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.common.PropertiesOptions;
import io.quarkus.cli.registry.ToggleRegistryClientMixin;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import picocli.CommandLine;

public class BaseBuildCommand {
    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Mixin
    protected ToggleRegistryClientMixin registryClient;

    @CommandLine.Mixin
    protected HelpOption helpOption;

    @CommandLine.ArgGroup(exclusive = false, validate = false)
    protected PropertiesOptions propertiesOptions = new PropertiesOptions();

    Path projectRoot;
    protected List<String> forcedExtensions = new ArrayList<>();

    public Path projectRoot() {
        if (projectRoot == null) {
            projectRoot = output.getTestDirectory();
            if (projectRoot == null) {
                projectRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
            }
        }
        return projectRoot;
    }

    public BuildSystemRunner getRunner() {
        BuildTool buildTool = QuarkusProjectHelper.detectExistingBuildTool(projectRoot()); // nullable
        return BuildSystemRunner.getRunner(output, propertiesOptions, registryClient, projectRoot(), buildTool);
    }

    public List<String> getForcedExtensions() {
        return forcedExtensions;
    }

    /**
     * Commands using `@ParentCommand` need to set the ouput.
     * This is needed for testing purposes.
     * More specifically --cli-test-dir relies on this.
     *
     * @param output The command ouput
     */
    public void setOutput(OutputOptionMixin output) {
        this.output = output;
    }

}
