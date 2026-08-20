package io.quarkus.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.quarkus.cli.common.BuildOptions;
import io.quarkus.cli.common.BuildToolContext;
import io.quarkus.cli.common.BuildToolDelegatingCommand;
import io.quarkus.cli.common.build.BuildSystemRunner;
import io.quarkus.devtools.project.BuildTool;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@CommandLine.Command(name = "run", sortOptions = false, mixinStandardHelpOptions = false, header = "Run application.")
public class Run extends BuildToolDelegatingCommand {

    private static final Map<BuildTool, String> ACTION_MAPPING = Map.of(BuildTool.MAVEN, "quarkus:run",
            BuildTool.GRADLE, "quarkusRun", BuildTool.GRADLE_KOTLIN_DSL, "quarkusRun");

    @CommandLine.Option(names = { "--target" }, description = "Run target.")
    String target;

    @CommandLine.Option(names = {
            "--also-build" }, description = "Build the project before running if not already built. True by default.", negatable = true, defaultValue = "true")
    boolean alsoBuild = true;

    @Override
    public void populateContext(BuildToolContext context) {
        super.populateContext(context);
        if (target != null)
            context.getPropertiesOptions().properties.put("quarkus.run.target", target);
    }

    @Override
    public Map<BuildTool, String> getActionMapping() {
        return ACTION_MAPPING;
    }

    @Override
    public void prepareMaven(BuildToolContext context) {
        BuildSystemRunner runner = getRunner(context);
        boolean cleanRequested = context.getBuildOptions().clean;
        boolean needsBuild = cleanRequested || (alsoBuild && !isMavenProjectBuilt(context));

        if (needsBuild) {
            output.info(cleanRequested ? "Clean build requested, building project first..."
                    : "Project not built yet, building automatically...");

            // Use 'install' for the build action (same as 'quarkus build')
            // Create separate build options that skip tests for faster builds
            BuildOptions buildOpts = new BuildOptions();
            buildOpts.clean = cleanRequested;
            buildOpts.offline = context.getBuildOptions().offline;
            buildOpts.buildNative = context.getBuildOptions().buildNative;
            buildOpts.skipTests = true; // Skip tests when building as part of run

            BuildSystemRunner.BuildCommandArgs buildArgs = runner.prepareAction("install", buildOpts,
                    context.getRunModeOption(),
                    context.getParams());

            if (context.getRunModeOption().isDryRun()) {
                output.info(" " + buildArgs.showCommand());
            } else {
                int buildExitCode = runner.run(buildArgs);
                if (buildExitCode != ExitCode.OK) {
                    throw new RuntimeException("Build failed with exit code: " + buildExitCode);
                }
            }

            // Reset clean flag so that the subsequent 'quarkus:run' action (executed by
            // the parent call() method) does NOT prepend another 'clean' phase.
            // This fixes the double-clean bug reported in #41380.
            context.getBuildOptions().clean = false;
        } else {
            // No build needed, but still need resources:resources for the run.
            // Use fresh BuildOptions without clean to avoid the double-clean bug.
            BuildOptions resourceOpts = new BuildOptions();
            resourceOpts.offline = context.getBuildOptions().offline;

            BuildSystemRunner.BuildCommandArgs compileArgs = runner.prepareAction("resources:resources",
                    resourceOpts,
                    context.getRunModeOption(),
                    context.getParams());

            if (getParentCommand().isPresent()) {
                return;
            } else if (context.getRunModeOption().isDryRun()) {
                output.info(" " + compileArgs.showCommand());
            } else {
                int compileExitCode = runner.run(compileArgs);
                if (compileExitCode != ExitCode.OK) {
                    throw new RuntimeException(
                            "Failed to compile. Compilation exited with exit code:" + compileExitCode);
                }
            }

            // Also reset clean here to prevent 'quarkus:run' from getting a spurious clean
            context.getBuildOptions().clean = false;
        }
    }

    /**
     * Checks if a Maven project has been built by looking for the target directory
     * with compiled output.
     */
    private boolean isMavenProjectBuilt(BuildToolContext context) {
        Path projectRoot = context.getProjectRoot();
        Path targetDir = projectRoot.resolve("target");
        if (!Files.isDirectory(targetDir)) {
            return false;
        }
        // Check for quarkus-app directory (fast-jar packaging) or classes directory
        Path quarkusApp = targetDir.resolve("quarkus-app");
        Path classes = targetDir.resolve("classes");
        return Files.isDirectory(quarkusApp) || Files.isDirectory(classes);
    }

    @Override
    public String toString() {
        return "Run {}";
    }
}
