package io.quarkus.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.quarkus.devtools.commands.UpdateProject;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.registry.RegistryResolutionException;

/**
 * Log Quarkus-related recommended updates, such as new Quarkus platform BOM versions and
 * Quarkus extensions versions that aren't managed by the Quarkus platform BOMs.
 */
@Mojo(name = "update", requiresProject = true)
public class UpdateMojo extends QuarkusProjectStateMojoBase {

    /**
     * Display information per project module.
     */
    @Parameter(property = "perModule")
    boolean perModule;

    /**
     * If true, instead of checking and recommending the latest available Quarkus platform version,
     * recommendations to properly align the current project configuration will be logged (if any)
     */

    /**
     * Version of the target platform (e.g: 2.0.0.Final)
     * You may instead use streamId to target the latest version of a specific platform stream.
     */
    @Parameter(property = "platformVersion", required = false)
    private String platformVersion;

    /**
     * Target streamId (e.g: 2.0)
     */
    @Parameter(property = "streamId", required = false)
    private String streamId;

    @Override
    protected void validateParameters() throws MojoExecutionException {
        getLog().warn("quarkus:update goal is experimental, its options and output might change in future versions");
        super.validateParameters();
    }

    @Override
    protected void processProjectState(QuarkusProject quarkusProject) throws MojoExecutionException {

        final UpdateProject invoker = new UpdateProject(quarkusProject);
        try {
            invoker.latestCatalog(getExtensionCatalogResolver().resolveExtensionCatalog());
        } catch (RegistryResolutionException e) {
            throw new MojoExecutionException(
                    "Failed to resolve the latest Quarkus extension catalog from the configured extension registries", e);
        }

        // TODO ALEXEY: resolve targetPlatformVersion from streamId if needed or from latest version
        invoker.targetPlatformVersion(platformVersion);
        invoker.perModule(perModule);
        invoker.appModel(resolveApplicationModel());

        try {
            invoker.execute();
        } catch (QuarkusCommandException e) {
            throw new MojoExecutionException("Failed to resolve the available updates", e);
        }
    }
}
