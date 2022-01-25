package io.quarkus.maven;

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.commands.handlers.InfoCommandHandler;
import io.quarkus.devtools.commands.handlers.UpdateCommandHandler;
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

        final Map<String, Object> params = new HashMap<>();
        params.put(UpdateCommandHandler.APP_MODEL, resolveApplicationModel());
        params.put(UpdateCommandHandler.LOG_STATE_PER_MODULE, perModule);

        final QuarkusCommandInvocation invocation = new QuarkusCommandInvocation(quarkusProject, params);
        QuarkusCommandOutcome outcome;
        try {
            outcome = new InfoCommandHandler().execute(invocation);
        } catch (QuarkusCommandException e) {
            throw new MojoExecutionException("Failed to resolve the available updates", e);
        }

        if (outcome.getValue(InfoCommandHandler.RECOMMENDATIONS_AVAILABLE, false)) {
            getLog().warn(
                    "Non-recommended Quarkus platform BOM and/or extension versions were found. For more details, please, execute 'mvn quarkus:update -Drectify'");
        }
    }
}
