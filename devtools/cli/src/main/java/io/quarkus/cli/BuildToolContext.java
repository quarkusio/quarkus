package io.quarkus.cli;

import java.nio.file.Path;
import java.util.List;

import io.quarkus.cli.common.BuildOptions;
import io.quarkus.cli.common.PropertiesOptions;
import io.quarkus.cli.common.RunModeOption;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProjectHelper;

/**
 * Context class that holds all the required info that is passed to {@link BuildToolDelegatingCommand}. In many cases a
 * subcommand delegates to the parent. It's cleaner to keep all the state in a single class to pass along.
 */
public class BuildToolContext {

    private final Path projectRoot;
    private final RunModeOption runModeOption;
    private final BuildOptions buildOptions;
    private final PropertiesOptions propertiesOptions;
    private final List<String> forcedExtensions;
    private final List<String> params;

    public BuildToolContext(Path projectRoot, RunModeOption runModeOption, BuildOptions buildOptions,
            PropertiesOptions propertiesOptions, List<String> forcedExtensions, List<String> params) {
        this.projectRoot = projectRoot;
        this.runModeOption = runModeOption;
        this.buildOptions = buildOptions;
        this.propertiesOptions = propertiesOptions;
        this.forcedExtensions = forcedExtensions;
        this.params = params;
    }

    public Path getProjectRoot() {
        return projectRoot;
    }

    public RunModeOption getRunModeOption() {
        return runModeOption;
    }

    public BuildOptions getBuildOptions() {
        return buildOptions;
    }

    public PropertiesOptions getPropertiesOptions() {
        return propertiesOptions;
    }

    public List<String> getForcedExtensions() {
        return forcedExtensions;
    }

    public List<String> getParams() {
        return params;
    }

    public BuildTool getBuildTool() {
        return QuarkusProjectHelper.detectExistingBuildTool(projectRoot); // nullable
    }
}
