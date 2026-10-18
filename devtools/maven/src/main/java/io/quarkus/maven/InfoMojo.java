package io.quarkus.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

import io.quarkus.devtools.commands.ProjectInfo;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.project.QuarkusProject;

/**
 * Log Quarkus-specific project information, such as imported Quarkus platform BOMs,
 * Quarkus extensions found among the project dependencies, etc.
 */
@Mojo(name = "info", requiresProject = true)
public class InfoMojo extends QuarkusProjectStateMojoBase {

    @Override
    protected void validateParameters() throws MojoExecutionException {
        getLog().warn("quarkus:info goal is experimental, its options and output may change in future versions");
        super.validateParameters();
    }

    @Override
    protected void processProjectState(QuarkusProject quarkusProject) throws MojoExecutionException {
        final ProjectInfo invoker = new ProjectInfo(quarkusProject);

        invoker.perModule(perModule);
        invoker.appModel(resolveApplicationModel());

        QuarkusCommandOutcome outcome;
        try {
            outcome = invoker.execute();
        } catch (QuarkusCommandException e) {
            throw new MojoExecutionException("Failed to resolve the available updates", e);
        }
    }
}
