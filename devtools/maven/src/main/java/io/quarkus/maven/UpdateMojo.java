package io.quarkus.maven;

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.handlers.UpdateCommandHandler;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.registry.RegistryResolutionException;

/**
 * Log Quarkus-related recommended updates, such as new Quarkus platform BOM versions and
 * Quarkus extensions versions that aren't managed by the Quarkus platform BOMs.
 */
@Mojo(name = "update", requiresProject = true)
public class UpdateMojo extends QuarkusProjectStateMojoBase {

    /**
     * If true, the state of the project will be logged as if the recommended updates
     * were applied
     */
    @Parameter(property = "recommendedState")
    boolean recommendedState;

    /**
     * If true, instead of checking and recommending the latest available Quarkus platform version,
     * recommendations to properly align the current project configuration will be logged (if any)
     */
    @Parameter(property = "rectify")
    boolean rectify;

    @Override
    protected void validateParameters() throws MojoExecutionException {
        getLog().warn("quarkus:update goal is experimental, its options and output might change in future versions");
        super.validateParameters();
    }

    @Override
    protected void processProjectState(QuarkusProject quarkusProject) throws MojoExecutionException {

        final Map<String, Object> params = new HashMap<>();
        try {
            params.put(UpdateCommandHandler.LATEST_CATALOG, getExtensionCatalogResolver().resolveExtensionCatalog());
        } catch (RegistryResolutionException e) {
            throw new MojoExecutionException(
                    "Failed to resolve the latest Quarkus extension catalog from the configured extension registries", e);
        }
        params.put(UpdateCommandHandler.APP_MODEL, resolveApplicationModel());
        params.put(UpdateCommandHandler.LOG_RECOMMENDED_STATE, recommendedState);
        params.put(UpdateCommandHandler.LOG_STATE_PER_MODULE, perModule);
        params.put(UpdateCommandHandler.RECTIFY, rectify);

        final QuarkusCommandInvocation invocation = new QuarkusCommandInvocation(quarkusProject, params);
        try {
            new UpdateCommandHandler().execute(invocation);
        } catch (QuarkusCommandException e) {
            throw new MojoExecutionException("Failed to resolve the available updates", e);
        }
    }
}
